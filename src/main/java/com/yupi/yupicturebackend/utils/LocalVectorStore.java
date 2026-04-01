package com.yupi.yupicturebackend.utils;

import java.util.ArrayList;
import java.util.List;

public class LocalVectorStore {
    private static final List<String> KNOWLEDGE_LIST = new ArrayList<>();

    // 初始化知识库
    public static void init() throws Exception {
        KNOWLEDGE_LIST.clear();
        KNOWLEDGE_LIST.addAll(DocumentParser.loadAllDocs());
        System.out.println("✅ 知识库加载完成：" + KNOWLEDGE_LIST.size() + " 条知识");
    }

    // 检索匹配（纯Java，无任何依赖报错）
    public static List<String> search(String question, int top) {
        List<String> result = new ArrayList<>();
        String q = question.toLowerCase();
        for (String doc : KNOWLEDGE_LIST) {
            if (doc.toLowerCase().contains(q.substring(0, Math.min(2, q.length())))) {
                result.add(doc);
                if (result.size() >= top) break;
            }
        }
        return result;
    }
}