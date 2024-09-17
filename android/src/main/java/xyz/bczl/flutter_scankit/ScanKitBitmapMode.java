package xyz.bczl.flutter_scankit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.hmsscankit.WriterException;
import com.huawei.hms.ml.scan.HmsBuildBitmapOption;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzer;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.huawei.hms.ml.scan.HmsScanFrame;
import com.huawei.hms.ml.scan.HmsScanFrameOptions;
import com.huawei.hms.mlsdk.common.MLFrame;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScanKitBitmapMode {

//    private static
    public static Map<String, Object> decode(Activity activity, @NonNull byte[] yuv,
                                             @NonNull Long width, @NonNull Long height,
                                             Map<String, Object> options) {
        Bitmap bitmap = convertToBitmap(width.intValue(), height.intValue(), yuv);
        HmsScanFrameOptions.Creator opt = new HmsScanFrameOptions.Creator();

        Object scanTypes = options.get("scanTypes");
        Object photoMode = options.get("photoMode");
        Object parseResult = options.get("parseResult");

        int[] args = ScanKitUtilities.getArrayFromFlags((Integer) scanTypes);
        int[] var2 = Arrays.copyOfRange(args, 1, args.length);
        HmsScanAnalyzer barcodeDetector = new HmsScanAnalyzer(new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(args[0], var2).create());
        MLFrame image = MLFrame.fromBitmap(bitmap);
        SparseArray<HmsScan> result = barcodeDetector.analyseFrame(image);

        HmsScan[] info = new HmsScan[result.size()];
        for (int index = 0; index < result.size(); index++) {
            info[index] = result.valueAt(index);
        }
        return getResult(info);
    }

    private static Bitmap convertToBitmap(int width, int height, byte[] data) {
        YuvImage yuv = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
        return BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().length);
    }

    static boolean checkHmsScan(HmsScan[] hmsScans){
        return hmsScans != null && hmsScans.length > 0;
    }

    public static byte[] encode(@NonNull String content, @NonNull Long width, @NonNull Long height, @NonNull Map<String, Object> options){
        HmsBuildBitmapOption.Creator creator = new HmsBuildBitmapOption.Creator();
        Object scanTypes = options.get("scanTypes");
        Object bgColor = options.get("bgColor");
        Object color = options.get("color");
        Object margin = options.get("margin");
        int type = HmsScan.QRCODE_SCAN_TYPE;
        if (scanTypes != null){
            type = ScanKitUtilities.getTypeFromFlags((Integer) scanTypes);
        }
        if (bgColor != null){
            creator.setBitmapBackgroundColor(((Long) bgColor).intValue());
        }
        if (color != null){
            creator.setBitmapColor(((Long) color).intValue());
        }
        if (margin != null){
            creator.setBitmapMargin((Integer) margin);
        }
        try {
            Bitmap bitmap = ScanUtil.buildBitmap(content, type,width.intValue(),height.intValue(),creator.create());
            return toByteArray(bitmap);
        } catch (WriterException e) {
            throw new ScanKitAPI.FlutterError("104",e.getMessage(),"");
        }
    }

    public static Map<String, Object> decodeBitmap(Context c, @NonNull byte[] data, @NonNull Map<String, Object> options) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0, data.length);
        HmsScanFrameOptions.Creator creator = new HmsScanFrameOptions.Creator();
        Object scanTypes = options.get("scanTypes");
        Object photoMode = options.get("photoMode");
        Object parseResult = options.get("parseResult");
        if (scanTypes != null){
            int[] args = ScanKitUtilities.getArrayFromFlags((Integer) scanTypes);
            int[] var2 = Arrays.copyOfRange(args, 1, args.length);
            creator.setHmsScanTypes(args[0],var2);
        }
        if (photoMode != null){
            creator.setPhotoMode((Boolean) photoMode);
        }
        if (parseResult != null){
            creator.setParseResult((Boolean) parseResult);
        }
        HmsScanFrame frame = new HmsScanFrame(bitmap);
        HmsScan[] hmsScans = ScanUtil.decode(c,frame,creator.setMultiMode(true).create()).getHmsScans();
        return getResult(hmsScans);
    }

    static Map<String, Object> getResult(HmsScan[] hmsScans){
        if(checkHmsScan(hmsScans)){
            HmsScan scan = hmsScans[0];
            if(TextUtils.isEmpty(scan.getOriginalValue())){
                BigDecimal zoomValue = new BigDecimal(Double.toString(scan.getZoomValue()));
                BigDecimal val = new BigDecimal("1.0");
                if (zoomValue.compareTo(val) > 0){
                    return new HashMap<>(Collections.singletonMap("zoomValue",scan.getZoomValue()));
                }
            }else {
                return new ScanResult(scan.getOriginalValue(), scan.getScanType()).toMap();
            }
        }
        return new HashMap<>();
    }

    static byte[] toByteArray(Bitmap bitmap){
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,bao);
        return bao.toByteArray();
    }
}
