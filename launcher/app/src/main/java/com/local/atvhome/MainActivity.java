package com.local.atvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "AtvHome";
    private static final int TOP = 0, DOCK = 1, GRID = 2, MENU = 3;
    private static final int COLS = 4;
    private static final float TILE_SCALE = 1.08f, PILL_SCALE = 1.06f;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private float dp;
    private int screenW, screenH, side, gap, tileW, tileH, labelH, rowH, bandH, menuW;
    private Typeface thin, light, regular;

    private FrameLayout root;
    private ScrollView scroller;
    private LinearLayout band;
    private RecyclerView grid;
    private GridLayoutManager gridLm;
    private final GridAdapter adapter = new GridAdapter();
    private GlassView glass;
    private TextView greeting, dockTitle;
    private GradientDrawable glowDrawable;
    private final TextView[] pills = new TextView[3];
    private LinearLayout menu;
    private final List<TextView> menuRows = new ArrayList<>();
    private final List<Runnable> menuActions = new ArrayList<>();

    private final List<AppEntry> dockApps = new ArrayList<>();
    private final List<View> dockTiles = new ArrayList<>();
    private final List<AppEntry> gridApps = new ArrayList<>();

    private int zone = DOCK, topIdx = 0, dockIdx = 0, gridIdx = 0, menuIdx = 0;
    private View scaled;
    private int glowColor = 0;
    private boolean resumed = false;
    private SharedPreferences prefs;
    private ConnectivityManager.NetworkCallback netCallback;

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
        dp = getResources().getDisplayMetrics().density;
        screenW = bounds.width();
        screenH = bounds.height();
        side = px(48);
        gap = px(22);
        tileW = (screenW - 2 * side - (COLS - 1) * gap) / COLS;
        tileH = tileW * 9 / 16;
        labelH = px(30);
        rowH = tileH + labelH + px(8);
        bandH = px(20) + tileH + labelH;
        menuW = px(220);
        thin = font(200);
        light = font(300);
        regular = font(400);

        buildUi();
        setContentView(root);
        loadApps();
        restoreFocus(prefs.getString("focus", null));
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> glass.follow());
        root.post(() -> applyFocus(false));

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packageReceiver, f, RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerNetwork();
    }

    @Override
    protected void onStop() {
        if (netCallback != null) {
            getSystemService(ConnectivityManager.class).unregisterNetworkCallback(netCallback);
            netCallback = null;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        updateGreeting();
        registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK), RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        resumed = false;
        unregisterReceiver(tickReceiver);
        String pkg = focusedPackage();
        if (pkg != null) prefs.edit().putString("focus", pkg).apply();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // HOME pressed while already home: back to the dock. Coming back from an app keeps focus.
        if (resumed) {
            menu.setVisibility(View.GONE);
            if (!dockApps.isEmpty()) zone = DOCK;
            applyFocus(true);
        }
    }

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateGreeting();
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            String keep = focusedPackage();
            loadApps();
            restoreFocus(keep);
            root.post(() -> applyFocus(false));
        }
    };

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setClipChildren(false);
        addWallpaper();

        scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setFocusable(false);
        scroller.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        scroller.setClipChildren(false);
        scroller.setOnScrollChangeListener((v, x, y, ox, oy) -> glass.follow());
        root.addView(scroller, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        scroller.addView(content, new FrameLayout.LayoutParams(MATCH, WRAP));

        content.addView(buildHero(), new LinearLayout.LayoutParams(MATCH, screenH - bandH - px(26)));

        band = new LinearLayout(this);
        band.setOrientation(LinearLayout.HORIZONTAL);
        band.setClipChildren(false);
        band.setClipToPadding(false);   // else the focused (scaled) dock tile is cut at the padding
        band.setPadding(side - px(24), px(20), 0, 0);
        GradientDrawable bandBg = new GradientDrawable();
        bandBg.setColor(0xE6161618);
        bandBg.setCornerRadius(px(28));
        bandBg.setStroke(Math.max(1, px(1) / 2), 0x14FFFFFF);
        band.setBackground(bandBg);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(MATCH, bandH);
        blp.leftMargin = blp.rightMargin = px(24);
        content.addView(band, blp);

        grid = new RecyclerView(this);
        gridLm = new GridLayoutManager(this, COLS);
        grid.setLayoutManager(gridLm);
        grid.setItemAnimator(null);
        grid.setNestedScrollingEnabled(false);
        grid.setFocusable(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setAdapter(adapter);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(COLS * (tileW + gap), WRAP);
        glp.leftMargin = side;
        glp.topMargin = px(44);
        glp.bottomMargin = px(60);
        content.addView(grid, glp);

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(px(6), px(6), px(6), px(6));
        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setColor(0xF2202023);
        menuBg.setCornerRadius(px(18));
        menuBg.setStroke(px(1), 0x2EFFFFFF);
        menu.setBackground(menuBg);
        menu.setVisibility(View.GONE);
        root.addView(menu, new FrameLayout.LayoutParams(menuW, WRAP));

        glass = new GlassView(this);
        root.addView(glass, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    private View buildHero() {
        FrameLayout hero = new FrameLayout(this);
        hero.setClipChildren(false);

        // Ambient glow in the focused dock app's colour, bleeding softly under the dock band
        glowDrawable = new GradientDrawable();
        glowDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        // The view is exactly two radii tall so the gradient reaches transparent before any edge.
        int glowR = px(300);
        glowDrawable.setGradientRadius(glowR);
        glowDrawable.setGradientCenter(0.22f, 0.5f);
        glowDrawable.setColors(new int[]{0, 0});
        View glow = new View(this);
        glow.setBackground(glowDrawable);
        FrameLayout.LayoutParams gl = new FrameLayout.LayoutParams(screenW, 2 * glowR, Gravity.BOTTOM | Gravity.START);
        gl.bottomMargin = -(glowR - px(50));   // centre sits just behind the dock title
        hero.addView(glow, gl);

        dockTitle = text(thin, 44, Color.WHITE);
        dockTitle.setSingleLine();
        dockTitle.setEllipsize(TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(screenW - 2 * side, WRAP, Gravity.BOTTOM | Gravity.START);
        tl.leftMargin = side;
        tl.bottomMargin = px(16);
        hero.addView(dockTitle, tl);

        LinearLayout clockCol = new LinearLayout(this);
        clockCol.setOrientation(LinearLayout.VERTICAL);
        greeting = text(light, 17, 0xB3FFFFFF);
        clockCol.addView(greeting);
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("H:mm");
        clock.setTypeface(thin);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80);
        clock.setTextColor(Color.WHITE);
        clock.setIncludeFontPadding(false);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(WRAP, WRAP);
        cl.topMargin = px(4);
        clockCol.addView(clock, cl);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE d MMMM");
        date.setFormat24Hour("EEEE d MMMM");
        date.setTypeface(light);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        date.setTextColor(0xB3FFFFFF);
        clockCol.addView(date);
        FrameLayout.LayoutParams ccl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.START);
        ccl.leftMargin = side;
        ccl.topMargin = px(36);
        hero.addView(clockCol, ccl);

        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setClipChildren(false);
        String[] names = {"Inputs", "Network", "Settings"};
        for (int i = 0; i < names.length; i++) {
            TextView p = text(regular, 14, Color.WHITE);
            p.setText(names[i]);
            p.setGravity(Gravity.CENTER);
            p.setPadding(px(18), 0, px(18), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(px(18));
            bg.setColor(0x29FFFFFF);
            bg.setStroke(px(1), 0x1FFFFFFF);
            p.setBackground(bg);
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(WRAP, px(36));
            if (i > 0) pl.leftMargin = px(12);
            pillRow.addView(p, pl);
            pills[i] = p;
        }
        FrameLayout.LayoutParams prl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.END);
        prl.rightMargin = side;
        prl.topMargin = px(40);
        hero.addView(pillRow, prl);
        return hero;
    }

    private void addWallpaper() {
        if (!getResources().getBoolean(R.bool.use_wallpaper)) return;
        int id = getResources().getIdentifier("wallpaper", "drawable", getPackageName());
        if (id == 0) return;
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageResource(id);
        iv.setColorFilter(Color.argb(getResources().getInteger(R.integer.wallpaper_dim), 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        root.addView(iv, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    private View createTile() {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setClipChildren(false);
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(px(12));
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.addView(new ImageView(this), new FrameLayout.LayoutParams(MATCH, MATCH));
        tile.addView(card, new LinearLayout.LayoutParams(tileW, tileH));
        TextView label = text(regular, 13, Color.WHITE);
        label.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        label.setPadding(0, px(9), 0, 0);
        label.setSingleLine();
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setAlpha(0f);
        tile.addView(label, new LinearLayout.LayoutParams(tileW, labelH));
        tile.setPivotX(tileW / 2f);
        tile.setPivotY(tileH / 2f);
        return tile;
    }

    private void bindTile(View tile, AppEntry e) {
        ViewGroup t = (ViewGroup) tile;
        ImageView img = (ImageView) ((ViewGroup) t.getChildAt(0)).getChildAt(0);
        TextView label = (TextView) t.getChildAt(1);
        if (e.banner != null) {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setPadding(0, 0, 0, 0);
            img.setImageDrawable(e.banner);
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setPadding(px(28), px(20), px(28), px(20));
            img.setImageDrawable(e.icon);
        }
        label.setText(e.label);
        tile.setScaleX(1f);
        tile.setScaleY(1f);
        label.setAlpha(0f);
    }

    private final class GridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return gridApps.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            FrameLayout cell = new FrameLayout(MainActivity.this);
            cell.setClipChildren(false);
            cell.setLayoutParams(new RecyclerView.LayoutParams(tileW + gap, rowH));
            cell.addView(createTile(), new FrameLayout.LayoutParams(tileW, tileH + labelH));
            return new RecyclerView.ViewHolder(cell) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            View tile = ((ViewGroup) h.itemView).getChildAt(0);
            if (tile == scaled) scaled = null;
            bindTile(tile, gridApps.get(pos));
        }
    }

    // ---------------------------------------------------------------- apps

    private void loadApps() {
        List<AppEntry> all = AppEntry.loadAll(this);
        dockApps.clear();
        gridApps.clear();
        for (String p : getResources().getStringArray(R.array.dock_packages)) {
            for (AppEntry e : all) {
                if (e.pkg.equals(p)) {
                    dockApps.add(e);
                    break;
                }
            }
        }
        for (AppEntry e : all) if (!dockApps.contains(e)) gridApps.add(e);

        band.removeAllViews();
        dockTiles.clear();
        for (AppEntry e : dockApps) {
            View t = createTile();
            bindTile(t, e);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH + labelH);
            lp.rightMargin = gap;
            band.addView(t, lp);
            dockTiles.add(t);
        }
        scaled = null;
        adapter.notifyDataSetChanged();
    }

    private String focusedPackage() {
        if (zone == DOCK && dockIdx < dockApps.size()) return dockApps.get(dockIdx).pkg;
        if (zone == GRID && gridIdx < gridApps.size()) return gridApps.get(gridIdx).pkg;
        return null;
    }

    private void restoreFocus(String pkg) {
        zone = !dockApps.isEmpty() ? DOCK : !gridApps.isEmpty() ? GRID : TOP;
        dockIdx = Math.min(dockIdx, Math.max(0, dockApps.size() - 1));
        gridIdx = Math.min(gridIdx, Math.max(0, gridApps.size() - 1));
        if (pkg == null) return;
        for (int i = 0; i < dockApps.size(); i++) {
            if (dockApps.get(i).pkg.equals(pkg)) {
                zone = DOCK;
                dockIdx = i;
                return;
            }
        }
        for (int i = 0; i < gridApps.size(); i++) {
            if (gridApps.get(i).pkg.equals(pkg)) {
                zone = GRID;
                gridIdx = i;
                return;
            }
        }
    }

    // ---------------------------------------------------------------- focus

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (down) move(0, -1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (down) move(0, 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (down) move(-1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (down) move(1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                // act on release so the key-up doesn't land in the app we just opened
                if (!down && !e.isCanceled()) activate();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (down && e.getRepeatCount() == 0) back();
                return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private void move(int dx, int dy) {
        int nDock = dockApps.size(), nGrid = gridApps.size();
        switch (zone) {
            case TOP:
                if (dx != 0) {
                    int n = clamp(topIdx + dx, 0, pills.length - 1);
                    if (n == topIdx) return;           // ends of the top bar don't fall through
                    topIdx = n;
                } else if (dy > 0 && nDock > 0) {
                    zone = DOCK;
                } else if (dy > 0 && nGrid > 0) {
                    zone = GRID;
                } else return;
                break;
            case DOCK:
                if (dx != 0) {
                    int n = clamp(dockIdx + dx, 0, nDock - 1);
                    if (n == dockIdx) return;
                    dockIdx = n;
                } else if (dy < 0) {
                    zone = TOP;
                } else if (nGrid > 0) {
                    zone = GRID;
                    gridIdx = Math.min(dockIdx, nGrid - 1);
                } else return;
                break;
            case GRID: {
                int row = gridIdx / COLS, col = gridIdx % COLS;
                if (dx != 0) {
                    int c = col + dx;
                    if (c < 0 || c >= COLS || gridIdx + dx >= nGrid) return;
                    gridIdx += dx;
                } else if (dy < 0) {
                    if (row > 0) gridIdx -= COLS;
                    else if (nDock > 0) {
                        zone = DOCK;
                        dockIdx = Math.min(col, nDock - 1);
                    } else zone = TOP;
                } else {
                    if (row == (nGrid - 1) / COLS) return;
                    gridIdx = Math.min(gridIdx + COLS, nGrid - 1);
                }
                break;
            }
            case MENU:
                if (dy == 0) return;
                int n = clamp(menuIdx + dy, 0, menuRows.size() - 1);
                if (n == menuIdx) return;
                menuIdx = n;
                break;
        }
        applyFocus(true);
    }

    private void activate() {
        switch (zone) {
            case TOP:
                if (topIdx == 0) openMenu();
                else if (topIdx == 1) startSafe(new Intent(Settings.ACTION_WIFI_SETTINGS));
                else startSafe(new Intent(Settings.ACTION_SETTINGS));
                break;
            case DOCK:
                if (dockIdx < dockApps.size()) startSafe(dockApps.get(dockIdx).launchIntent());
                break;
            case GRID:
                if (gridIdx < gridApps.size()) startSafe(gridApps.get(gridIdx).launchIntent());
                break;
            case MENU:
                Runnable r = menuActions.get(menuIdx);
                closeMenu();
                r.run();
                break;
        }
    }

    private void back() {
        if (zone == MENU) closeMenu();
        else if ((zone == GRID || zone == TOP) && !dockApps.isEmpty()) {
            zone = DOCK;
            applyFocus(true);
        }
    }

    private View focusView() {
        switch (zone) {
            case TOP:
                return pills[topIdx];
            case DOCK:
                return dockIdx < dockTiles.size() ? dockTiles.get(dockIdx) : null;
            case GRID: {
                View cell = gridLm.findViewByPosition(gridIdx);
                return cell == null ? null : ((ViewGroup) cell).getChildAt(0);
            }
            case MENU:
                return menuIdx < menuRows.size() ? menuRows.get(menuIdx) : null;
        }
        return null;
    }

    private void applyFocus(boolean animate) {
        View v = focusView();
        if (v == null && zone == GRID) {     // grid not laid out yet
            root.post(() -> applyFocus(false));
            return;
        }
        if (scaled != null && scaled != v) setScaled(scaled, false);
        if (v != null) setScaled(v, true);
        scaled = v;

        if (zone == GRID) scrollToRow(gridIdx / COLS);
        else if (zone != MENU) scroller.smoothScrollTo(0, 0);

        if (dockIdx < dockApps.size()) showDockApp(dockApps.get(dockIdx));
        dockTitle.animate().alpha(zone == DOCK ? 1f : 0.45f).setDuration(200);

        glass.moveTo(v == null ? null : targetFor(v), animate);
    }

    private void setScaled(View v, boolean on) {
        boolean pill = isPill(v);
        boolean row = menuRows.contains(v);
        if (row) return;
        float s = on ? (pill ? PILL_SCALE : TILE_SCALE) : 1f;
        v.animate().scaleX(s).scaleY(s).setDuration(180);
        if (pill) {
            ((GradientDrawable) v.getBackground()).setColor(on ? 0x4DFFFFFF : 0x29FFFFFF);
        } else {
            ((ViewGroup) v).getChildAt(1).animate().alpha(on ? 1f : 0f).setDuration(180);
        }
    }

    private boolean isPill(View v) {
        for (View p : pills) if (p == v) return true;
        return false;
    }

    private GlassView.Target targetFor(View v) {
        final boolean pill = isPill(v);
        final boolean row = menuRows.contains(v);
        final boolean tile = !pill && !row;
        final View box = tile ? ((ViewGroup) v).getChildAt(0) : v;
        final float scale = tile ? TILE_SCALE : pill ? PILL_SCALE : 1f;
        final float outset = px(tile ? 5 : 3);
        return new GlassView.Target() {
            final int[] p = new int[2], g = new int[2];

            @Override
            public void rect(RectF out) {
                ((View) v.getParent()).getLocationInWindow(p);
                glass.getLocationInWindow(g);
                float l = p[0] - g[0] + v.getLeft() + (box == v ? 0 : box.getLeft());
                float t = p[1] - g[1] + v.getTop() + (box == v ? 0 : box.getTop());
                float cx = l + box.getWidth() / 2f, cy = t + box.getHeight() / 2f;
                float hw = box.getWidth() * scale / 2f + outset, hh = box.getHeight() * scale / 2f + outset;
                out.set(cx - hw, cy - hh, cx + hw, cy + hh);
            }

            @Override
            public float radius() {
                float base = tile ? px(12) : pill ? box.getHeight() / 2f : px(10);
                return base * scale + outset;
            }
        };
    }

    private void scrollToRow(int row) {
        int y = grid.getTop() - px(40) + Math.max(0, row - 1) * rowH;
        int max = Math.max(0, scroller.getChildAt(0).getHeight() - scroller.getHeight());
        scroller.smoothScrollTo(0, clamp(y, 0, max));
    }

    private void showDockApp(AppEntry e) {
        dockTitle.setText(e.label);
        int c = e.glowColor();
        if (c == glowColor) return;
        int from = glowColor == 0 ? c : glowColor;
        glowColor = c;
        ValueAnimator a = ValueAnimator.ofObject(new ArgbEvaluator(), from, c);
        a.setDuration(380);
        a.addUpdateListener(an -> {
            int col = (int) an.getAnimatedValue() & 0xFFFFFF;
            glowDrawable.setColors(new int[]{col | 0x66000000, col | 0x1F000000, col});
        });
        a.start();
    }

    // ---------------------------------------------------------------- inputs menu

    private void openMenu() {
        buildMenuRows();
        if (menuRows.isEmpty()) return;
        TextView pill = pills[0];
        int[] p = new int[2], r = new int[2];
        pill.getLocationInWindow(p);
        root.getLocationInWindow(r);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menu.getLayoutParams();
        lp.leftMargin = Math.min(p[0] - r[0], screenW - menuW - px(24));
        lp.topMargin = p[1] - r[1] + pill.getHeight() + px(12);
        menu.setLayoutParams(lp);
        menu.setVisibility(View.VISIBLE);
        menu.setAlpha(0f);
        menu.setTranslationY(-px(8));
        menu.animate().alpha(1f).translationY(0).setDuration(160);
        zone = MENU;
        menuIdx = 0;
        menu.post(() -> applyFocus(true));
    }

    private void closeMenu() {
        menu.setVisibility(View.GONE);
        zone = TOP;
        applyFocus(true);
    }

    private void buildMenuRows() {
        menu.removeAllViews();
        menuRows.clear();
        menuActions.clear();
        TvInputManager tim = getSystemService(TvInputManager.class);
        String tuner = null;
        if (tim != null) {
            List<TvInputInfo> list = new ArrayList<>(tim.getTvInputList());
            list.sort((a, b) -> {
                int ra = rank(a.getType()), rb = rank(b.getType());
                if (ra != rb) return Integer.compare(ra, rb);
                return label(a).compareTo(label(b));
            });
            for (TvInputInfo info : list) {
                if (info.getParentId() != null || info.isHidden(this)) continue;
                int type = info.getType();
                if (type == TvInputInfo.TYPE_TUNER) {
                    // Live TV = digital antenna first; analog last (Thai broadcast TV is DVB-T2)
                    if (tuner == null || tunerRank(info.getId()) < tunerRank(tuner)) tuner = info.getId();
                    continue;
                }
                if (type != TvInputInfo.TYPE_HDMI && type != TvInputInfo.TYPE_COMPOSITE) continue;
                boolean connected = tim.getInputState(info.getId()) != TvInputManager.INPUT_STATE_DISCONNECTED;
                String label = type == TvInputInfo.TYPE_COMPOSITE && label(info).toLowerCase().contains("composite") ? "AV" : label(info);
                Uri uri = TvContract.buildChannelUriForPassthroughInput(info.getId());
                addMenuRow(label, connected, () -> openInTvApp(uri));
            }
        }
        final String tunerId = tuner;
        addMenuRow("Live TV", true, () -> openInTvApp(tunerId == null ? null : TvContract.buildChannelsUriForInput(tunerId)));
    }

    private static int tunerRank(String id) {
        String s = id.toLowerCase();
        return s.contains("analog") ? 2 : s.contains("cable") ? 1 : 0;
    }

    private static int rank(int type) {
        return type == TvInputInfo.TYPE_HDMI ? 0 : type == TvInputInfo.TYPE_COMPOSITE ? 1 : 2;
    }

    private String label(TvInputInfo info) {
        CharSequence c = info.loadCustomLabel(this);
        return String.valueOf(c != null && c.length() > 0 ? c : info.loadLabel(this));
    }

    private void addMenuRow(String label, boolean connected, Runnable action) {
        TextView t = text(regular, 15, Color.WHITE);
        t.setText(label);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(px(16), 0, px(16), 0);
        t.setAlpha(connected ? 1f : 0.45f);
        menu.addView(t, new LinearLayout.LayoutParams(MATCH, px(42)));
        menuRows.add(t);
        menuActions.add(action);
    }

    private void openInTvApp(Uri uri) {
        ComponentName tvApp = new ComponentName(getString(R.string.tv_app_package), getString(R.string.tv_app_activity));
        if (uri != null && startSafe(new Intent(Intent.ACTION_VIEW, uri).setComponent(tvApp))) return;
        Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(tvApp.getPackageName());
        if (launch != null) startSafe(launch);
    }

    // ---------------------------------------------------------------- network + greeting

    private void registerNetwork() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        netCallback = new ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
            @Override
            public void onCapabilitiesChanged(@NonNull Network n, @NonNull NetworkCapabilities caps) {
                String name = "Connected";
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    name = "Ethernet";
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    name = "Wi-Fi";
                    if (caps.getTransportInfo() instanceof WifiInfo) {
                        String ssid = ((WifiInfo) caps.getTransportInfo()).getSSID();
                        if (ssid != null && !ssid.contains("unknown ssid")) name = ssid.replace("\"", "");
                    }
                }
                final String label = name;
                runOnUiThread(() -> setNetworkLabel(label));
            }

            @Override
            public void onLost(@NonNull Network n) {
                runOnUiThread(() -> setNetworkLabel("Offline"));
            }
        };
        cm.registerDefaultNetworkCallback(netCallback);
    }

    private void setNetworkLabel(String s) {
        if (s.equals(pills[1].getText().toString())) return;
        pills[1].setText(s);
        pills[1].post(() -> glass.follow());
    }

    private void updateGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(h < 5 ? "Good night" : h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : h < 22 ? "Good evening" : "Good night");
    }

    // ---------------------------------------------------------------- helpers

    private boolean startSafe(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception ex) {
            Log.w(TAG, "Could not start " + i, ex);
            return false;
        }
    }

    private Typeface font(int weight) {
        try {
            return new Typeface.Builder(getAssets(), "fonts/Outfit.ttf")
                    .setFontVariationSettings("'wght' " + weight)
                    .build();
        } catch (Exception e) {
            return Typeface.create("sans-serif-light", Typeface.NORMAL);
        }
    }

    private TextView text(Typeface tf, float sp, int color) {
        TextView t = new TextView(this);
        t.setTypeface(tf);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
