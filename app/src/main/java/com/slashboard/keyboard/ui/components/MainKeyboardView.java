package com.slashboard.keyboard.ui.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.slashboard.keyboard.R;

/**
 * MainKeyboardView: Custom rendering engine for Spacebar Crimson/Ruby Red Pill
 * with dynamic custom branding text and Enter Key Blue Pill.
 */
public class MainKeyboardView extends View {

    public interface OnKeyActionListener {
        void onSpacePress();
        void onEnterPress();
        void onKeyPress(String text);
    }

    private final Paint spacebarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enterKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF spacebarRect = new RectF();
    private final RectF enterKeyRect = new RectF();

    private String brandingText = "Slashboard";
    private Drawable returnIconDrawable;
    private OnKeyActionListener actionListener;
    private float cornerRadiusPx;

    public MainKeyboardView(Context context) {
        this(context, null);
    }

    public MainKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        cornerRadiusPx = 24 * density;

        // Spacebar Crimson / Ruby Red Pill Paint (#990000 to #B30000)
        spacebarPaint.setStyle(Paint.Style.FILL);
        spacebarPaint.setColor(Color.parseColor("#B30000"));

        // Enter Key Vibrant Blue Pill paint
        enterKeyPaint.setStyle(Paint.Style.FILL);
        enterKeyPaint.setColor(Color.parseColor("#0066FF"));

        // Text Branding Paint (Clean Sans-Serif / Roboto, White #FFFFFF, 14sp bold/semi-bold centered)
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        returnIconDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_keyboard_return);
        reloadBranding();
    }

    public void setOnKeyActionListener(OnKeyActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * Dynamically reads custom label from SharedPreferences with strict default to "Slashboard".
     */
    public void reloadBranding() {
        SharedPreferences prefs = getContext().getSharedPreferences(
                getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String label = prefs.getString("custom_spacebar_label", null);
        if (label == null || label.trim().isEmpty()) {
            SharedPreferences slashboardPrefs = getContext().getSharedPreferences("slashboard_prefs", Context.MODE_PRIVATE);
            label = slashboardPrefs.getString("custom_spacebar_label", "Slashboard");
        }
        this.brandingText = (label != null && !label.trim().isEmpty()) ? label : "Slashboard";
        invalidate();
    }

    public void setCustomBrandingText(String text) {
        this.brandingText = (text != null && !text.trim().isEmpty()) ? text : "Slashboard";
        invalidate();
    }

    public String getCustomBrandingText() {
        return brandingText;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        float padding = 4 * density;
        float height = h - 2 * padding;

        // Spacebar occupies the central pill area
        float spaceWidth = w * 0.55f;
        float spaceLeft = (w - spaceWidth) / 2.0f;
        spacebarRect.set(spaceLeft, padding, spaceLeft + spaceWidth, padding + height);

        // Enter key occupies the right-side blue pill area
        float enterWidth = w * 0.18f;
        float enterLeft = w - enterWidth - padding;
        enterKeyRect.set(enterLeft, padding, enterLeft + enterWidth, padding + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw Spacebar Crimson / Ruby Red Pill (#990000 to #B30000)
        canvas.drawRoundRect(spacebarRect, cornerRadiusPx, cornerRadiusPx, spacebarPaint);

        // 2. Draw centered custom branding text (White, 14sp)
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textY = spacebarRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2;
        canvas.drawText(brandingText, spacebarRect.centerX(), textY, textPaint);

        // 3. Draw Enter Key Blue Pill (#0066FF)
        canvas.drawRoundRect(enterKeyRect, cornerRadiusPx, cornerRadiusPx, enterKeyPaint);

        // 4. Draw Return Arrow Icon on Enter Key
        if (returnIconDrawable != null) {
            int iconSize = (int) (22 * getResources().getDisplayMetrics().density);
            int left = (int) (enterKeyRect.centerX() - iconSize / 2);
            int top = (int) (enterKeyRect.centerY() - iconSize / 2);
            returnIconDrawable.setBounds(left, top, left + iconSize, top + iconSize);
            returnIconDrawable.setTint(Color.WHITE);
            returnIconDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            if (spacebarRect.contains(x, y)) {
                if (actionListener != null) {
                    actionListener.onSpacePress();
                }
                return true;
            } else if (enterKeyRect.contains(x, y)) {
                if (actionListener != null) {
                    actionListener.onEnterPress();
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}
