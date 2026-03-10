package com.azizul.tushereditor.editor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdtHelper {

    private static final String[] KEYWORDS = {
        "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", 
        "continue", "default", "do", "double", "else", "enum", "extends", "final", 
        "finally", "float", "for", "if", "implements", "import", "instanceof", 
        "int", "interface", "long", "native", "new", "package", "private", 
        "protected", "public", "return", "short", "static", "super", "switch", 
        "synchronized", "this", "throw", "throws", "try", "void", "volatile", "while",
        "String", "System", "out", "println", "print", "Bundle", "Override", "View",
        "Context", "Intent"
    };

    public static List<String> getSuggestions(String source, int cursorIndex) {
        List<String> suggestions = new ArrayList<>();
        String prefix = getPrefix(source, cursorIndex);
        if (prefix.isEmpty()) return suggestions;

        Set<String> resultSet = new HashSet<>();

        for (String keyword : KEYWORDS) {
            if (keyword.toLowerCase().startsWith(prefix.toLowerCase())) {
                resultSet.add(keyword);
            }
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            
            cu.findAll(VariableDeclarator.class).forEach(v -> {
                String name = v.getNameAsString();
                if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
                    resultSet.add(name);
                }
            });

            cu.findAll(CallableDeclaration.class).forEach(m -> {
                String name = m.getNameAsString();
                if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
                    resultSet.add(name + "();");
                }
            });

            cu.findAll(TypeDeclaration.class).forEach(t -> {
                String name = t.getNameAsString();
                if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
                    resultSet.add(name);
                }
            });

        } catch (Exception e) {
            Pattern methodPattern = Pattern.compile("\\b" + prefix + "[a-zA-Z0-9_]*(?=\\s*\\()");
            Matcher matcher = methodPattern.matcher(source);
            while (matcher.find()) {
                resultSet.add(matcher.group() + "();");
            }
            
            Pattern wordPattern = Pattern.compile("\\b" + prefix + "[a-zA-Z0-9_]*\\b");
            matcher = wordPattern.matcher(source);
            while (matcher.find()) {
                String word = matcher.group();
                if (!word.equalsIgnoreCase(prefix)) resultSet.add(word);
            }
        }

        suggestions.addAll(resultSet);
        return suggestions;
    }

    private static String getPrefix(String source, int cursorIndex) {
        if (source == null || cursorIndex <= 0) return "";
        int start = cursorIndex;
        while (start > 0 && Character.isJavaIdentifierPart(source.charAt(start - 1))) {
            start--;
        }
        return source.substring(start, cursorIndex);
    }
}
