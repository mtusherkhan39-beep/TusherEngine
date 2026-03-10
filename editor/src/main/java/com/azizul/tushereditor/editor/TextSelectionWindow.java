package com.azizul.tushereditor.editor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSelectionWindow {
    private final TusherEngine engine;
    private final PopupWindow popupWindow;
    private final View mainView;
    private final View generateView;
    public boolean justDismissed = false;
    private float lastX, lastY; 

    public TextSelectionWindow(TusherEngine engine) {
        this.engine = engine;
        Context context = engine.getContext();
        
        mainView = LayoutInflater.from(context).inflate(R.layout.popup_selection_menu, null);
        generateView = LayoutInflater.from(context).inflate(R.layout.popup_generate_menu, null);
        
        popupWindow = new PopupWindow(mainView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(25f);

        popupWindow.setOnDismissListener(() -> {
            justDismissed = true;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> justDismissed = false, 200);
        });

        initMainButtons();
        initGenerateButtons();
    }

    private void initMainButtons() {
        View btnCopy = mainView.findViewById(R.id.btn_copy);
        View btnCut = mainView.findViewById(R.id.btn_cut);
        View btnPaste = mainView.findViewById(R.id.btn_paste);
        View btnSelectAll = mainView.findViewById(R.id.btn_select_all);
        View btnMore = mainView.findViewById(R.id.btn_more_options);

        if (btnCopy != null) btnCopy.setOnClickListener(v -> { copyToClipboard(); hide(); });
        if (btnCut != null) btnCut.setOnClickListener(v -> { copyToClipboard(); deleteSelection(); hide(); });
        if (btnPaste != null) btnPaste.setOnClickListener(v -> { pasteFromClipboard(); hide(); });
        if (btnSelectAll != null) btnSelectAll.setOnClickListener(v -> {
            engine.textSelection.setSelection(0, engine.textContent.length());
            show(engine.getWidth() / 2f, engine.getHeight() / 3f);
        });

        if (btnMore != null) btnMore.setOnClickListener(v -> {
            // ১. কন্টেন্ট পরিবর্তন করা (dismiss না করে)
            updateGenerateButtonVisibility();
            popupWindow.setContentView(generateView);
            
            // ২. নতুন কন্টেন্ট মেজার করে পপআপ আপডেট করা
            generateView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int nw = generateView.getMeasuredWidth();
            int nh = generateView.getMeasuredHeight();
            
            // ৩. একই জায়গায় রিসাইজ করে আপডেট করা
            int px = (int) (lastX - nw / 2);
            int py = (int) (lastY - nh - (45 * engine.scaleFactor));
            popupWindow.update(px, py, nw, nh);
        });
    }

    private void initGenerateButtons() {
        View btnComment = generateView.findViewById(R.id.btn_comment);
        View btnConstructor = generateView.findViewById(R.id.btn_create_constructor);
        View btnGetterSetter = generateView.findViewById(R.id.btn_create_getter_setter);
        View btnToString = generateView.findViewById(R.id.btn_create_tostring);

        if (btnComment != null) btnComment.setOnClickListener(v -> { toggleComment(); hide(); });
        if (btnConstructor != null) btnConstructor.setOnClickListener(v -> { generateConstructor(); hide(); });
        if (btnGetterSetter != null) btnGetterSetter.setOnClickListener(v -> { generateGetterSetter(); hide(); });
        if (btnToString != null) btnToString.setOnClickListener(v -> { generateToString(); hide(); });
    }

    private void updateGenerateButtonVisibility() {
        boolean isJava = (engine.getCurrentLanguage() == TusherEngine.Language.JAVA);
        boolean hasVars = !getSelectedVariables(false).isEmpty();
        int visibility = (isJava && hasVars) ? View.VISIBLE : View.GONE;

        generateView.findViewById(R.id.btn_create_constructor).setVisibility(visibility);
        generateView.findViewById(R.id.btn_create_getter_setter).setVisibility(visibility);
        generateView.findViewById(R.id.btn_create_tostring).setVisibility(visibility);
        generateView.findViewById(R.id.divider_java).setVisibility(isJava ? View.VISIBLE : View.GONE);
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public void show(float x, float y) {
        if (popupWindow.isShowing()) popupWindow.dismiss();
        justDismissed = false;

        popupWindow.setContentView(mainView);
        
        int[] screenPos = new int[2];
        engine.getLocationInWindow(screenPos);
        this.lastX = screenPos[0] + x;
        this.lastY = screenPos[1] + y;

        boolean isJava = engine.getCurrentLanguage() == TusherEngine.Language.JAVA;
        mainView.findViewById(R.id.btn_more_options).setVisibility(isJava ? View.VISIBLE : View.GONE);
        mainView.findViewById(R.id.divider_generate).setVisibility(isJava ? View.VISIBLE : View.GONE);

        mainView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int w = mainView.getMeasuredWidth();
        int h = mainView.getMeasuredHeight();

        float scale = engine.scaleFactor;
        int px = (int) (lastX - w / 2);
        int py = (int) (lastY - h - (45 * scale)); 

        if (py < screenPos[1] + 10) py = (int)lastY + (int)(50 * scale);
        px = Math.max(30, Math.min(px, engine.getContext().getResources().getDisplayMetrics().widthPixels - w - 30));

        popupWindow.showAtLocation(engine, Gravity.NO_GRAVITY, px, py);
    }

    public void hide() {
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
    }

    private void toggleComment() {
        int start = Math.min(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        int end = Math.max(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        if (start == end) return;
        String text = engine.textContent.substring(start, end);
        boolean isXml = engine.getCurrentLanguage() == TusherEngine.Language.XML;
        String open = isXml ? "<!--" : "/*";
        String close = isXml ? "-->" : "*/";
        if (text.startsWith(open) && text.endsWith(close)) {
            String unCommented = text.substring(open.length(), text.length() - close.length());
            engine.textContent.replace(start, end, unCommented);
            engine.textSelection.setSelection(start, start + unCommented.length());
        } else {
            String commented = open + text + close;
            engine.textContent.replace(start, end, commented);
            engine.textSelection.setSelection(start, start + commented.length());
        }
        engine.updateState(true);
    }

    private void generateToString() {
        List<VariableInfo> vars = getSelectedVariables(true);
        if (vars.isEmpty()) return;
        String className = getClassNameFromContent();
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n    @Override\n    public String toString() {\n");
        sb.append("        return \"").append(className).append("{\" +\n");
        for (int i = 0; i < vars.size(); i++) {
            VariableInfo v = vars.get(i);
            sb.append("                \"").append(i > 0 ? ", " : "").append(v.name).append("='\" + ").append(v.name).append(" + '\\'' +\n");
        }
        sb.append("                '}';\n    }");
        engine.textContent.insert(findClassInsertPoint(), sb.toString());
        engine.updateState(true);
    }

    private String getClassNameFromContent() {
        String content = engine.textContent.toString();
        Pattern pattern = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "MyClass";
    }

    private int findClassInsertPoint() {
        String content = engine.textContent.toString();
        int lastBrace = content.lastIndexOf('}');
        return (lastBrace != -1) ? lastBrace : content.length();
    }

    private void generateConstructor() {
        List<VariableInfo> vars = getSelectedVariables(true);
        if (vars.isEmpty()) return;
        String className = getClassNameFromContent();
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n    public ").append(className).append("(");
        for (int i = 0; i < vars.size(); i++) {
            sb.append(vars.get(i).type).append(" ").append(vars.get(i).name).append(i < vars.size() - 1 ? ", " : "");
        }
        sb.append(") {\n");
        for (VariableInfo info : vars) sb.append("        this.").append(info.name).append(" = ").append(info.name).append(";\n");
        sb.append("    }");
        engine.textContent.insert(findClassInsertPoint(), sb.toString());
        engine.updateState(true);
    }

    private void generateGetterSetter() {
        List<VariableInfo> vars = getSelectedVariables(true);
        if (vars.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (VariableInfo info : vars) {
            String capName = info.name.substring(0, 1).toUpperCase() + info.name.substring(1);
            String getter = (info.type.equals("boolean") && info.name.startsWith("is")) ? info.name : "get" + capName;
            sb.append("\n\n    public ").append(info.type).append(" ").append(getter).append("() {\n        return ").append(info.name).append(";\n    }\n\n");
            sb.append("    public void set").append(capName).append("(").append(info.type).append(" ").append(info.name).append(") {\n        this.").append(info.name).append(" = ").append(info.name).append(";\n    }");
        }
        engine.textContent.insert(findClassInsertPoint(), sb.toString());
        engine.updateState(true);
    }

    private List<VariableInfo> getSelectedVariables(boolean showToast) {
        List<VariableInfo> vars = new ArrayList<>();
        int start = Math.min(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        int end = Math.max(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        String source = (start != end) ? engine.textContent.substring(start, end) : "";
        if (source.isEmpty()) {
            int cursor = engine.cursorIndex;
            int lineStart = engine.textContent.lastIndexOf("\n", cursor - 1) + 1;
            int lineEnd = engine.textContent.indexOf("\n", cursor);
            if (lineEnd == -1) lineEnd = engine.textContent.length();
            source = engine.textContent.substring(lineStart, lineEnd);
        }
        
        Pattern pattern = Pattern.compile("(?:(?:public|private|protected|static|final)\\s+)*([A-Za-z0-9<>_\\[\\]]+)\\s+([A-Za-z0-9_]+)\\s*;?");
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if (!type.equals("return") && !type.equals("package") && !type.equals("class")) {
                vars.add(new VariableInfo(type, name));
            }
        }
        if (vars.isEmpty() && showToast) Toast.makeText(engine.getContext(), "No variables found!", Toast.LENGTH_SHORT).show();
        return vars;
    }

    private static class VariableInfo {
        String type, name;
        VariableInfo(String t, String n) { type = t; name = n; }
    }

    private void copyToClipboard() {
        int start = Math.min(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        int end = Math.max(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        if (start == end) return;
        ClipboardManager cb = (ClipboardManager) engine.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("TusherEngine", engine.textContent.substring(start, end)));
    }

    private void deleteSelection() {
        int start = Math.min(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        int end = Math.max(engine.textSelection.selectionStart, engine.textSelection.selectionEnd);
        engine.textContent.delete(start, end);
        engine.textSelection.setSelection(start, start);
        engine.updateState(true);
    }

    private void pasteFromClipboard() {
        ClipboardManager cb = (ClipboardManager) engine.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb.hasPrimaryClip() && cb.getPrimaryClip().getItemCount() > 0) {
            CharSequence t = cb.getPrimaryClip().getItemAt(0).getText();
            if (t != null) {
                if (engine.textSelection.isSelectionActive()) deleteSelection();
                engine.textContent.insert(engine.cursorIndex, t);
                int p = engine.cursorIndex + t.length();
                engine.textSelection.setSelection(p, p);
                engine.updateState(true);
            }
        }
    }
}
