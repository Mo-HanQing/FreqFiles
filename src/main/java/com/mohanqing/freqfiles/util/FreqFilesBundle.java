package com.mohanqing.freqfiles.util;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * 国际化资源管理类
 * 强制使用英文资源，忽略语言环境设置
 */
public final class FreqFilesBundle {
    @NonNls
    private static final String BUNDLE_PATH = "/messages/FreqFilesBundle.properties";
    
    // 强制使用英文资源文件的 Properties
    private static final Properties ENGLISH_PROPERTIES;
    
    static {
        ENGLISH_PROPERTIES = new Properties();
        try {
            // 直接加载英文资源文件，不经过 ResourceBundle 的语言检测
            InputStream stream = FreqFilesBundle.class.getResourceAsStream(BUNDLE_PATH);
            if (stream != null) {
                ENGLISH_PROPERTIES.load(stream);
                stream.close();
            }
        } catch (Exception e) {
            // 如果加载失败，使用空 Properties
            e.printStackTrace();
        }
    }

    private FreqFilesBundle() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取消息，强制使用英文资源
     * 始终加载 FreqFilesBundle.properties（英文），忽略语言环境设置
     */
    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = "messages.FreqFilesBundle") String key, Object... params) {
        try {
            String message = ENGLISH_PROPERTIES.getProperty(key, key);
            if (params != null && params.length > 0) {
                // 替换占位符 {0}, {1}, ...
                for (int i = 0; i < params.length; i++) {
                    message = message.replace("{" + i + "}", String.valueOf(params[i]));
                }
            }
            return message;
        } catch (Exception e) {
            // 如果出错，返回 key 本身
            return key;
        }
    }

    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = "messages.FreqFilesBundle") String key, Object... params) {
        return () -> message(key, params);
    }
}

