package com.local.atvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One launchable TV app: its banner (or icon), label and a colour sampled from the banner. */
final class AppEntry {
    final String pkg;
    final ComponentName component;
    final String label;
    final Drawable banner;   // null when the app only has an icon
    final Drawable icon;
    private int glow = 0;    // 0 = not sampled yet

    private AppEntry(String pkg, ComponentName component, String label, Drawable banner, Drawable icon) {
        this.pkg = pkg;
        this.component = component;
        this.label = label;
        this.banner = banner;
        this.icon = icon;
    }

    Intent launchIntent() {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    /** Most saturated, reasonably bright colour of the banner, for the ambient glow. */
    int glowColor() {
        if (glow != 0) return glow;
        Drawable shown = banner != null ? banner : icon;
        // draw a copy: setBounds on the shared drawable would shrink the tile's banner
        Drawable d = shown == null || shown.getConstantState() == null ? null
                : shown.getConstantState().newDrawable().mutate();
        int best = 0xFF5E5CE6;
        if (d != null) {
            Bitmap bmp = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, 32, 18);
            d.draw(c);
            float bestScore = 0.12f;
            float[] hsv = new float[3];
            for (int y = 0; y < 18; y++) {
                for (int x = 0; x < 32; x++) {
                    int p = bmp.getPixel(x, y);
                    if (Color.alpha(p) < 128) continue;
                    Color.colorToHSV(p, hsv);
                    if (hsv[2] < 0.25f) continue;
                    float score = hsv[1] * hsv[2];
                    if (score > bestScore) { bestScore = score; best = p | 0xFF000000; }
                }
            }
            bmp.recycle();
            // Normalise brightness so dark brand colours still glow
            Color.colorToHSV(best, hsv);
            hsv[2] = Math.max(hsv[2], 0.85f);
            best = Color.HSVToColor(hsv);
        }
        glow = best;
        return glow;
    }

    /** All apps with a TV launcher entry, sorted by name, without this launcher. */
    static List<AppEntry> loadAll(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(q, 0);
        List<AppEntry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : ris) {
            ActivityInfo ai = ri.activityInfo;
            if (ai.packageName.equals(ctx.getPackageName()) || !seen.add(ai.packageName)) continue;
            Drawable banner = ai.loadBanner(pm);
            if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
            Drawable icon = banner == null ? ai.loadIcon(pm) : null;
            String label = String.valueOf(ri.loadLabel(pm));
            out.add(new AppEntry(ai.packageName, new ComponentName(ai.packageName, ai.name), label, banner, icon));
        }
        Collator col = Collator.getInstance();
        out.sort((a, b) -> col.compare(a.label, b.label));
        return out;
    }
}
