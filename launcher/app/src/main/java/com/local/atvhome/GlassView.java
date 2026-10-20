package com.local.atvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * The single translucent "glass" focus highlight. It covers the whole screen but only draws
 * one rounded rectangle, which springs from its current place to wherever the target is now
 * (re-read every frame, so it stays glued to a tile while the page scrolls).
 */
final class GlassView extends View {

    /** Where the highlight should be, in this view's coordinates; called every frame. */
    interface Target {
        void rect(RectF out);
        float radius();
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cur = new RectF();
    private final RectF from = new RectF();
    private final RectF to = new RectF();
    private float radius, fromRadius;
    private Target target;
    private final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
    private boolean placed = false;

    GlassView(Context ctx) {
        super(ctx);
        float dp = ctx.getResources().getDisplayMetrics().density;
        fill.setColor(0x24FFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.5f * dp);
        stroke.setColor(0x66FFFFFF);
        anim.setDuration(420);
        anim.setInterpolator(new OvershootInterpolator(1.3f));
        anim.addUpdateListener(a -> invalidate());
        setWillNotDraw(false);
    }

    void moveTo(Target t, boolean animate) {
        target = t;
        if (!placed || !animate) {
            anim.cancel();
            placed = true;
            from.setEmpty();
            invalidate();
            return;
        }
        from.set(cur);
        fromRadius = radius;
        anim.cancel();
        anim.start();
    }

    /** Re-read the target position (e.g. while scrolling). */
    void follow() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (target == null) return;
        target.rect(to);
        float r = target.radius();
        if (anim.isRunning() && !from.isEmpty()) {
            float f = (float) anim.getAnimatedValue(); // already overshoot-interpolated
            cur.set(lerp(from.left, to.left, f), lerp(from.top, to.top, f),
                    lerp(from.right, to.right, f), lerp(from.bottom, to.bottom, f));
            radius = lerp(fromRadius, r, Math.min(1f, f));
        } else {
            cur.set(to);
            radius = r;
        }
        if (cur.width() <= 0) return;
        c.drawRoundRect(cur, radius, radius, fill);
        // light from above: a soft sheen on the top half
        sheen.setShader(new LinearGradient(0, cur.top, 0, cur.centerY(), 0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(cur, radius, radius, sheen);
        c.drawRoundRect(cur, radius, radius, stroke);
    }

    private static float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }
}
