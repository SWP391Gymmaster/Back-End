package com.swp391_be.service;

import java.text.Normalizer;
import java.util.Locale;

/** Chuẩn hóa tên bộ môn để so sánh: "Group X", "group-x", "GroupX" đều thành GROUP_X. */
public final class SubjectKey {

    private SubjectKey() {
    }

    public static String of(String subject) {
        if (subject == null) {
            return "";
        }
        String s = Normalizer.normalize(subject.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replace('đ', 'd').replace('Đ', 'D');
        s = s.replaceAll("[\\s\\-]+", "_").toUpperCase(Locale.ROOT);
        return s.equals("GROUPX") ? "GROUP_X" : s;
    }
}
