package com.zncm.dminter.mmhelper.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.github.mrengineer13.snackbar.SnackBar;
import com.malinskiy.materialicons.IconDrawable;
import com.malinskiy.materialicons.Iconify;
import com.zncm.dminter.mmhelper.Constant;
import com.zncm.dminter.mmhelper.MyApplication;
import com.zncm.dminter.mmhelper.OpenInentActivity;
import com.zncm.dminter.mmhelper.R;
import com.zncm.dminter.mmhelper.autocommand.AndroidCommand;
import com.zncm.dminter.mmhelper.data.CardInfo;
import com.zncm.dminter.mmhelper.data.EnumInfo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by dminter on 2016/7/19.
 */

public class Xutils {
    public static String getTimeTodayYMD() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }
    //获取应用主界面
    public static String getLaunchClassNameByPkName(Context context, String pkgName) {
        Intent intent = isExit(context, pkgName);
        if (intent == null) {
            exec("pm enable " + pkgName);
            intent = isExit(context, pkgName);
        }
        return intent.getComponent().getClassName();
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(float dpValue) {
        final float scale = MyApplication.getInstance().ctx.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(float pxValue) {
        final float scale = MyApplication.getInstance().ctx.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static void openUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MyApplication.getInstance().ctx.startActivity(it);
        } catch (Exception e) {

        }
    }

    public static String exec(String command) {
        // execute a shell command, returning output in a string
        try {
            Runtime rt = Runtime.getRuntime();
            Process process = rt.exec("su");
//            Process process = rt.exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            process.waitFor();

            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void snTip(Activity activity, String string) {
        new SnackBar.Builder(activity)
                .withMessage(string)
                .withStyle(SnackBar.Style.INFO)
                .withBackgroundColorId(R.color.colorPrimary)
                .withDuration(SnackBar.MED_SNACK)
                .show();
    }

    public static void copyText(Context ctx, String text) {
        ClipboardManager cbm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cbm.setText(text);
    }

    public static int cmdExe(String cmdEnd) {
        String commands = cmdEnd;
        int ret = AndroidCommand.noRoot;
        if (Xutils.isNotEmptyOrNull(commands)) {
            ret = AndroidCommand.execRooted(commands);
            if (ret == AndroidCommand.noRoot) {
                try {
                    commands = commands.substring(0, commands.lastIndexOf("/"));
                    commands = commands.replaceAll("am start -n ", "");
                    startAppByPackageName(MyApplication.getInstance().getApplicationContext(), commands, Constant.attempt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }


    public static void sendToDesktop(Activity activity, CardInfo cardInfo) {
        sendToDesktop(activity, cardInfo, false);
    }


    public static void sendToDesktop(Activity activity, CardInfo cardInfo, boolean isShotcut) {

        Drawable drawable = null;
        if (cardInfo.getType() == EnumInfo.cType.URL.getValue()) {
            drawable = activity.getResources().getDrawable(R.mipmap.ic_url);
        } else if (cardInfo.getType() == EnumInfo.cType.CMD.getValue()) {
            drawable = activity.getResources().getDrawable(R.mipmap.ic_shell);
        } else if (cardInfo.getType() == EnumInfo.cType.SHORT_CUT_SYS.getValue()) {
            drawable = activity.getResources().getDrawable(R.mipmap.ic_shortcut);
        }
        Intent intent;
        BitmapDrawable bitmapDrawable;
        intent = new Intent();
        Intent shortIntent = new Intent(activity, OpenInentActivity.class);
        shortIntent.setAction("android.intent.action.VIEW");
        shortIntent.putExtra("android.intent.extra.UID", 0);
        shortIntent.putExtra("pkName", cardInfo.getPackageName());
        shortIntent.putExtra("className", cardInfo.getClassName());
        shortIntent.putExtra("cardId", cardInfo.getId());
        shortIntent.putExtra("isShotcut", isShotcut);
        shortIntent.putExtra("random", new Random().nextLong());
        intent.putExtra("android.intent.extra.shortcut.INTENT", shortIntent);
        intent.putExtra("android.intent.extra.shortcut.NAME", makeShortcutIconTitle(cardInfo.getTitle()));
        bitmapDrawable = (BitmapDrawable) drawable;
        if (bitmapDrawable == null) {
            try {
                intent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.fromContext(activity.createPackageContext(cardInfo.getPackageName(), 0), getAppIconId(cardInfo.getPackageName())));
            } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
                localNameNotFoundException.printStackTrace();
            }
        } else {
            if (bitmapDrawable == null) {
                bitmapDrawable = (BitmapDrawable) activity.getResources().getDrawable(R.mipmap.ic_launcher);
            }
            intent.putExtra("android.intent.extra.shortcut.ICON", bitmapDrawable.getBitmap());

        }
        intent.putExtra("duplicate", false);
        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        activity.sendBroadcast(intent);
        tShort(Constant.add_shortcut);
    }

    // 发送到桌面快捷方式
    public static void sendToDesktop(Activity ctx, Class<?> mClass, String title, Drawable drawable, String pkName, String className, int cardId) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        Bitmap bitmap = null;
        if (bitmapDrawable == null) {
            bitmapDrawable = (BitmapDrawable) ctx.getResources().getDrawable(R.mipmap.ic_launcher);
        }
        if (bitmapDrawable == null) {
            return;
        }
        bitmap = bitmapDrawable.getBitmap();
        Intent sender = new Intent();
        Intent shortcutIntent = new Intent(ctx, mClass);
        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(Intent.EXTRA_UID, 0);
        shortcutIntent.putExtra("pkName", pkName);
        shortcutIntent.putExtra("className", className);
        shortcutIntent.putExtra("cardId", cardId);
        sender.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        sender.putExtra(Intent.EXTRA_SHORTCUT_NAME, makeShortcutIconTitle(title));
        sender.putExtra(Intent.EXTRA_SHORTCUT_ICON,
                bitmap);
//        sender.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
//                Intent.ShortcutIconResource.fromContext(ctx, resId));
        sender.putExtra("duplicate", true);
        sender.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        ctx.sendBroadcast(sender);
    }

    // 创建快捷方式
    public static String makeShortcutIconTitle(String content) {
        final int SHORTCUT_ICON_TITLE_MAX_LEN = 10;
        final String TAG_CHECKED = String.valueOf('\u221A');
        final String TAG_UNCHECKED = String.valueOf('\u25A1');
        content = content.replace(TAG_CHECKED, "");
        content = content.replace(TAG_UNCHECKED, "");
        return content.length() > SHORTCUT_ICON_TITLE_MAX_LEN ? content.substring(0,
                SHORTCUT_ICON_TITLE_MAX_LEN) : content;
    }

    public static void sendTo(Context ctx, String sendWhat) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sendWhat);
        ctx.startActivity(shareIntent);
    }

//    int attempt = 3;

    /**
     * 通过packagename启动应用
     *
     * @param context
     * @param packagename
     */
    public static void startAppByPackageName(Context context, String packagename, int attempt) {
        try {
            Intent intent = isExit(context, packagename);
            context.startActivity(intent);
        } catch (Exception e) {
            exec(Constant.common_pm_e_p + packagename);
            if (attempt > 0) {
                startAppByPackageName(context, packagename, --attempt);
            }
        }

    }


    public static Intent isExit(Context context, String pk_name) {
        if (isEmptyOrNull(pk_name)) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        Intent it = packageManager.getLaunchIntentForPackage(pk_name);
        return it;
    }

    public static List<String> importTxt(Context context, int rawId) {
        List<String> dataList = new ArrayList<String>();
        BufferedReader br = null;
        try {
            //new FileReader(file)
            InputStream is = context.getResources().openRawResource(rawId);
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = "";
            while ((line = br.readLine()) != null) {
                dataList.add(line);
            }
        } catch (Exception e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                    br = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return dataList;
    }


    public static void initIndicatorTheme(PagerSlidingTabStrip indicator) {
        Context ctx = MyApplication.getInstance().ctx;
        indicator.setTextColor(ctx.getResources().getColor(R.color.material_light_white));
        indicator.setIndicatorColor(ctx.getResources().getColor(R.color.material_light_white));
        indicator.setBackgroundColor(ctx.getResources().getColor(R.color.colorPrimary));
        indicator.setUnderlineHeight(1);
        indicator.setIndicatorHeight(4);
    }

    public static ApplicationInfo getAppInfo(String pName) {
        ApplicationInfo info;
        try {
            PackageManager pm = MyApplication.getInstance().ctx.getPackageManager();
            info = pm.getApplicationInfo(pName, 0);
            return info;
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
        return null;

    }


    /*
   * 获取程序 图标
   */
    public static Drawable getAppIcon(String packname) {
        try {
            PackageManager pm = MyApplication.getInstance().ctx.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packname, 0);
            return info.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
        return null;
    }

    public static int getAppIconId(String packname) {
        try {
            PackageManager pm = MyApplication.getInstance().ctx.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packname, 0);
            return info.icon;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        }
        return R.mipmap.ic_launcher;
    }

    /*
     *获取程序的版本号
     */
    public String getAppVersion(String packname) {

        try {
            PackageManager pm = MyApplication.getInstance().ctx.getPackageManager();
            PackageInfo packinfo = pm.getPackageInfo(packname, 0);
            return packinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        }
        return null;
    }


    public static void tShort(String msg) {
        if (isEmptyOrNull(msg)) {
            return;
        }
        Toast.makeText(MyApplication.getInstance().ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void tLong(String msg) {
        if (isEmptyOrNull(msg)) {
            return;
        }
        Toast.makeText(MyApplication.getInstance().ctx, msg, Toast.LENGTH_LONG).show();
    }

    /*
     * 获取程序的名字
     */
    public String getAppName(String packname) {
        try {
            PackageManager pm = MyApplication.getInstance().ctx.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packname, 0);
            return info.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
        return null;
    }

    public static Drawable getApkIcon(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch (OutOfMemoryError e) {
            }
        }
        return null;
    }

    public static <T> boolean listNotNull(List<T> t) {
        if (t != null && t.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static void debug(Object object) {
        try {
            Log.d("MMHelper", String.valueOf(object));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }


    public static Drawable initIconWhite(Iconify.IconValue iconValue) {
        return new IconDrawable(MyApplication.getInstance().ctx, iconValue).colorRes(R.color.material_light_white).sizeDp(24);
    }


    public static boolean isEmptyOrNull(String string) {

        return string == null || string.trim().length() == 0 || string.equals("null");


    }

    public static boolean isNotEmptyOrNull(String string) {
        return !isEmptyOrNull(string);

    }


    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    public static String getTimeYMDHM_(Long inputDate) {
        if (inputDate == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy_MM_dd_HH_mm").format(new Date(inputDate));
    }


    public static void writeTxtFile(String path, String newStr) {
        FileOutputStream fos = null;
        try {
            byte[] bom = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};

            File file = new File(path);
            if (file.exists()) {
                delFile(file);
            }
            file.createNewFile();
            fos = new FileOutputStream(file);

            fos.write(bom);
            fos.write(newStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean saveBitmap(Bitmap bitmap, String outputPath) {
        try {
            FileOutputStream fileOutputStream = null;
            File file = new File(outputPath);
            if (file.exists()) {
                delFile(file);
            }
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getPathFromUri(Context context, Uri uri) {
        String path = "";
        if (uri == null || "content://media/external/file/-1".equals(uri.toString())) {
            tShort("文件选取失败~");
            return null;
        }
        String[] projection = {"_data"};
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {

                        path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                    }
                    cursor.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static boolean delFile(File file) {
        return file != null && file.exists() ? (file.isDirectory() ? false : file.delete()) : false;
    }


}
