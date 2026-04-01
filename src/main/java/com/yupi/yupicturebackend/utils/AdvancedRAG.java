package com.yupi.yupicturebackend.utils;

import java.util.List;

public class AdvancedRAG {
    private static boolean ready = false;

    public static void init() throws Exception {
        LocalVectorStore.init();
        ready = true;
    }

    public static String getKnowledge(String question) {
        if (!ready || question.isBlank()) return "";
        List<String> list = LocalVectorStore.search(question, 3);
        return list.isEmpty() ? "" : "参考资料：\n" + String.join("\n", list);
    }
}