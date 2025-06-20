package dev.applicazza.flutter.plugins.whatsapp_stickers_plus;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.flutter.plugin.common.MethodCall;
import io.flutter.util.PathUtils;

import static dev.applicazza.flutter.plugins.whatsapp_stickers_plus.StickerPackLoader.getStickerAssetUri;

public class ConfigFileManager {

    public static final String CONTENT_FILE_NAME = "sticker_packs.json";

    static List<StickerPack> getStickerPacks(Context context) {
        List<StickerPack> stickerPackList;
        File file = new File(getConfigFilePath(context));
        if (!file.exists()) {
            return new ArrayList<StickerPack>();
        }
        try (InputStream contentsInputStream = new FileInputStream(file)) {
            stickerPackList = ContentFileParser.parseStickerPacks(contentsInputStream);
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("config file has some issues hahaha: " + e.getMessage(), e);
        }
        return stickerPackList;
    }

    static StickerPack fromMethodCall(Context context, MethodCall call) {
        String identifier = call.argument("identifier");
        String name = call.argument("name");
        String publisher = call.argument("publisher");
        String trayImageFileName = call.argument("trayImageFileName");
        trayImageFileName = getFileName(trayImageFileName);
        String publisherWebsite = call.argument("publisherWebsite");
        String privacyPolicyWebsite = call.argument("privacyPolicyWebsite");
        String licenseAgreementWebsite = call.argument("licenseAgreementWebsite");
        String androidPlayStoreLink = call.argument("androidPlayStoreLink");
        String iosAppStoreLink = call.argument("iosAppStoreLink");
        Map<String, List<String>> stickers = call.argument("stickers");
        boolean animatedStickerPack = call.argument("animatedStickerPack");
        String imageDataVersion = call.argument("imageDataVersion");
        StickerPack newStickerPack = new StickerPack(identifier, name, publisher, trayImageFileName, "",
                publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite, imageDataVersion, false, animatedStickerPack);
        List<Sticker> newStickers = new ArrayList<Sticker>();
        assert stickers != null;
        
        // Log original order
        Log.d("ConfigFileManager", "Original sticker order:");
        int index = 0;
        for (Map.Entry<String, List<String>> entry : stickers.entrySet()) {
            Log.d("ConfigFileManager", (index + 1) + ". " + entry.getKey());
            Sticker s = new Sticker(getFileName(entry.getKey()), entry.getValue(), "");
            newStickers.add(s);
            index++;
        }
        
        // Sort stickers alphabetically by filename
        Collections.sort(newStickers, new Comparator<Sticker>() {
            @Override
            public int compare(Sticker s1, Sticker s2) {
                return s1.imageFileName.compareToIgnoreCase(s2.imageFileName);
            }
        });
        
        // Log sorted order
        Log.d("ConfigFileManager", "Sorted sticker order (alphabetically):");
        for (int i = 0; i < newStickers.size(); i++) {
            Log.d("ConfigFileManager", (i + 1) + ". " + newStickers.get(i).imageFileName);
        }
        
        newStickerPack.setStickers(newStickers);
        newStickerPack.setAndroidPlayStoreLink(androidPlayStoreLink);
        newStickerPack.setIosAppStoreLink(iosAppStoreLink);
        return newStickerPack;
    }

    static boolean addNewPack(Context context, StickerPack stickerPack) throws JSONException, InvalidPackException {
        List<StickerPack> stickerPacks = new ArrayList<StickerPack>();
        for (StickerPack s : getStickerPacks(context)) {
            if (!s.identifier.equals(stickerPack.identifier)) {
                stickerPacks.add(s);
            }
        }
        stickerPacks.add(stickerPack);
        return updateConfigFile(context, stickerPacks);
    }

    static String getFileName(String name) {
        if (name.contains("assets://")) {
            name = name.replace("assets://", "");
            name = name.replace("/", "_MZN_AD_");
        } else if (name.contains("file://")) {
            name = name.replace("file://", "");
            name = name.replace("/", "_MZN_FD_");
        }
        return name;
    }

    static boolean updateConfigFile(Context context, List<StickerPack> stickerPacks)
            throws JSONException, InvalidPackException {
        JSONObject mObj = new JSONObject();
        if (stickerPacks.size() <= 0) {
            mObj.put("android_play_store_link", "");
            mObj.put("ios_app_store_link", "");
        } else {
            mObj.put("android_play_store_link", stickerPacks.get(0).androidPlayStoreLink);
            mObj.put("ios_app_store_link", stickerPacks.get(0).iosAppStoreLink);
        }
        JSONArray _packs = new JSONArray();
        for (StickerPack s : stickerPacks) {
            JSONObject obj = new JSONObject();
            obj.put("identifier", s.identifier);
            obj.put("name", s.name);
            obj.put("publisher", s.publisher);
            obj.put("tray_image_file", getFileName(s.trayImageFile));
            obj.put("image_data_version", s.imageDataVersion);
            obj.put("avoid_cache", s.avoidCache);
            obj.put("publisher_email", s.publisherEmail);
            obj.put("publisher_website", s.publisherWebsite);
            obj.put("privacy_policy_website", s.privacyPolicyWebsite);
            obj.put("license_agreement_website", s.licenseAgreementWebsite);
            obj.put("animated_sticker_pack", s.animatedStickerPack);

            JSONArray stickerList = new JSONArray();
            for (Sticker _sticker : s.getStickers()) {
                JSONObject stickerObj = new JSONObject();
                String stickerFileName = getFileName(_sticker.imageFileName);
                stickerObj.put("image_file", stickerFileName);
                JSONArray _emojies = new JSONArray();
                for (String emoji : _sticker.emojis) {
                    _emojies.put(emoji);
                }
                stickerObj.put("emojis", _emojies);
                stickerList.put(stickerObj);
            }
            obj.put("stickers", stickerList);
            _packs.put(obj);
        }
        mObj.put("sticker_packs", _packs);
        writeConfigFile(context, mObj.toString());
        return true;
    }

    static void writeConfigFile(Context context, String jsonString) {
        String filePath = getConfigFilePath(context);
        File f = new File(filePath);
        try {
            FileWriter writer = new FileWriter(f);
            writer.write(jsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void generateConfigFile(Context context) throws JSONException {
        File file = new File(getConfigFilePath(context));
        if (!file.exists()) {
            // prepare data
            JSONObject mObj = new JSONObject();
            mObj.put("android_play_store_link", "");
            mObj.put("ios_app_store_link", "");
            JSONArray _packs = new JSONArray();
            mObj.put("sticker_packs", _packs);
            // write in file
            writeConfigFile(context, mObj.toString());
        }
    }

    public static String getConfigFilePath(Context context) {
        return PathUtils.getDataDirectory(context) + File.separator + CONTENT_FILE_NAME;
    }
}
