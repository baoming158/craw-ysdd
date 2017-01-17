package com.yssd.craw.util;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;



/**
 * 编码识别辅助类
 * 
 * @ 说明：根据字节数组或者输入流推断文档的编码
 */
public class CodeDetector {

    private static Logger log = LoggerFactory.getLogger(CodeDetector.class);

    /**
     * 由字节数组推断编码
     * 
     * @param data
     * @return
     */
    public static String getEncode(byte[] data) {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(data);
        CharsetMatch match = detector.detect();
        String encoding = match.getName();
        log.debug("侦测到的编码为:{}", encoding);
        return encoding;
    }

    /**
     * 由输入流推断编码
     * 
     * @param data
     * @return
     */
    public static String getEncode(InputStream data) throws IOException {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(data);
        CharsetMatch match = detector.detect();
        String encoding = match.getName();
        log.debug("侦测到的编码为:{}", encoding);
        return encoding;
    }
}
