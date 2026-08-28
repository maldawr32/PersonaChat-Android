package com.maldawr.chatsimulator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

public final class CustomizationHelper {
    private static final String NOTIFICATION_LARGE = "custom_notification_large.png";
    private static final String NOTIFICATION_SMALL = "custom_notification_small.png";
    private static final String HOME_ICON = "custom_home_icon.png";
    private static final String SHORTCUT_ID = "chat_simulator_custom_home";

    private CustomizationHelper() {}

    public static boolean saveNotificationIcon(Context context, Uri uri) {
        try {
            Bitmap source = decode(context, uri);
            if (source == null) return false;
            Bitmap square = centerCrop(source, 320);
            write(context, NOTIFICATION_LARGE, square);
            Bitmap small = buildSmallNotificationIcon(square, 128);
            write(context, NOTIFICATION_SMALL, small);
            if (source != square && !source.isRecycled()) source.recycle();
            if (!square.isRecycled()) square.recycle();
            if (!small.isRecycled()) small.recycle();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean saveHomeIcon(Context context, Uri uri) {
        try {
            Bitmap source = decode(context, uri);
            if (source == null) return false;
            Bitmap square = centerCrop(source, 512);
            Bitmap marked = addSimulationBadge(square);
            write(context, HOME_ICON, marked);
            if (source != square && !source.isRecycled()) source.recycle();
            if (!square.isRecycled()) square.recycle();
            if (!marked.isRecycled()) marked.recycle();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasNotificationIcon(Context context) {
        return file(context, NOTIFICATION_SMALL).exists() && file(context, NOTIFICATION_LARGE).exists();
    }

    public static boolean hasHomeIcon(Context context) {
        return file(context, HOME_ICON).exists();
    }

    public static void resetNotificationIcon(Context context) {
        file(context, NOTIFICATION_SMALL).delete();
        file(context, NOTIFICATION_LARGE).delete();
    }

    public static Icon notificationSmallIcon(Context context) {
        Bitmap custom = load(context, NOTIFICATION_SMALL);
        if (custom != null) return Icon.createWithBitmap(custom);
        return Icon.createWithResource(context, R.drawable.ic_notification_custom);
    }

    public static Bitmap notificationLargeBitmap(Context context) {
        return load(context, NOTIFICATION_LARGE);
    }

    public static Bitmap homeIconBitmap(Context context) {
        return load(context, HOME_ICON);
    }

    public static boolean publishOrUpdateHomeShortcut(Context context) {
        Bitmap custom = load(context, HOME_ICON);
        if (custom == null) return false;
        ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        if (manager == null) return false;

        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setComponent(new ComponentName(context.getPackageName(), context.getPackageName() + ".ShortcutEntry"));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String fullLabel = DisplayPrefs.getHomeTitle(context);
        String shortLabel = fullLabel.length() > 18 ? fullLabel.substring(0, 18) : fullLabel;
        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, SHORTCUT_ID)
                .setShortLabel(shortLabel)
                .setLongLabel(fullLabel + " • Simulation")
                .setIcon(Icon.createWithAdaptiveBitmap(custom))
                .setIntent(open)
                .build();

        try {
            boolean alreadyPinned = false;
            List<ShortcutInfo> pinned = manager.getPinnedShortcuts();
            for (ShortcutInfo item : pinned) {
                if (SHORTCUT_ID.equals(item.getId())) {
                    alreadyPinned = true;
                    break;
                }
            }
            manager.addDynamicShortcuts(Collections.singletonList(shortcut));
            if (alreadyPinned) {
                manager.updateShortcuts(Collections.singletonList(shortcut));
                return true;
            }
            if (manager.isRequestPinShortcutSupported()) {
                return manager.requestPinShortcut(shortcut, null);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static Bitmap decode(Context context, Uri uri) throws Exception {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
        }
        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
    }

    private static Bitmap centerCrop(Bitmap source, int size) {
        int w = source.getWidth();
        int h = source.getHeight();
        int side = Math.min(w, h);
        int left = (w - side) / 2;
        int top = (h - side) / 2;
        Bitmap crop = Bitmap.createBitmap(source, left, top, side, side);
        return Bitmap.createScaledBitmap(crop, size, size, true);
    }

    private static Bitmap buildSmallNotificationIcon(Bitmap source, int size) {
        Bitmap scaled = Bitmap.createScaledBitmap(source, size, size, true);
        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int bg = averageCorners(scaled);
        int bgR = Color.red(bg), bgG = Color.green(bg), bgB = Color.blue(bg);
        int[] pixels = new int[size * size];
        scaled.getPixels(pixels, 0, size, 0, 0, size, size);
        long alphaSum = 0;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int sourceAlpha = Color.alpha(p);
            int dr = Color.red(p) - bgR;
            int dg = Color.green(p) - bgG;
            int db = Color.blue(p) - bgB;
            int distance = (int) Math.sqrt(dr * dr + dg * dg + db * db);
            int derived = Math.max(0, Math.min(255, (distance - 10) * 5));
            int alpha = sourceAlpha < 245 ? sourceAlpha : derived;
            pixels[i] = Color.argb(alpha, 255, 255, 255);
            alphaSum += alpha;
        }
        if (alphaSum < (long) size * size * 255 / 30) {
            Canvas fallback = new Canvas(result);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            fallback.drawCircle(size / 2f, size / 2f, size * 0.34f, p);
        } else {
            result.setPixels(pixels, 0, size, 0, 0, size, size);
        }
        Canvas canvas = new Canvas(result);
        float r = size * 0.18f;
        float cx = size - r - size * 0.05f;
        float cy = size - r - size * 0.05f;
        Paint badge = new Paint(Paint.ANTI_ALIAS_FLAG);
        badge.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, r, badge);
        Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        cut.setColor(Color.TRANSPARENT);
        cut.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        cut.setTextAlign(Paint.Align.CENTER);
        cut.setTextSize(r * 1.25f);
        cut.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
        Paint.FontMetrics fm = cut.getFontMetrics();
        canvas.drawText("S", cx, cy - (fm.ascent + fm.descent) / 2f, cut);
        cut.setXfermode(null);
        if (scaled != source && !scaled.isRecycled()) scaled.recycle();
        return result;
    }

    private static Bitmap addSimulationBadge(Bitmap source) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        float size = result.getWidth();
        float radius = size * 0.145f;
        float cx = size - radius - size * 0.055f;
        float cy = size - radius - size * 0.055f;
        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        circle.setColor(Color.rgb(11, 20, 26));
        canvas.drawCircle(cx, cy, radius, circle);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(radius * 0.72f);
        Paint.FontMetrics fm = text.getFontMetrics();
        canvas.drawText("SIM", cx, cy - (fm.ascent + fm.descent) / 2f, text);
        return result;
    }

    private static int averageCorners(Bitmap bitmap) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] values = {bitmap.getPixel(2, 2), bitmap.getPixel(w - 3, 2), bitmap.getPixel(2, h - 3), bitmap.getPixel(w - 3, h - 3)};
        int r = 0, g = 0, b = 0;
        for (int p : values) { r += Color.red(p); g += Color.green(p); b += Color.blue(p); }
        return Color.rgb(r / values.length, g / values.length, b / values.length);
    }

    private static void write(Context context, String name, Bitmap bitmap) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file(context, name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        }
    }

    private static Bitmap load(Context context, String name) {
        File f = file(context, name);
        if (!f.exists()) return null;
        try { return BitmapFactory.decodeFile(f.getAbsolutePath()); }
        catch (Exception ignored) { return null; }
    }

    private static File file(Context context, String name) {
        return new File(context.getFilesDir(), name);
    }
}
