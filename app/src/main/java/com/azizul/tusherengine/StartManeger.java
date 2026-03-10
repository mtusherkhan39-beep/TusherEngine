package com.azizul.tusherengine;

import android.content.Context;

public class StartManeger {
    private static StartManeger instance;
    private final Context context;

    private StartManeger(Context context) {
        this.context = context.getApplicationContext();
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new StartManeger(context);
        }
    }

    public static StartManeger getInstance() {
        if (instance == null) {
            // যদি ApplicationLoader এভেইলেবল থাকে তবে অটো-ইনিশিয়ালাইজ করা
            if (AppliCaionLoader.getInstance() != null) {
                init(AppliCaionLoader.getInstance());
            } else {
                throw new RuntimeException("StartManeger must be initialized in Application class");
            }
        }
        return instance;
    }

    // এখন আপনি সরাসরি StartManeger.getContext() ব্যবহার করতে পারবেন
    public static Context getContext() {
        return getInstance().context;
    }

    public void startApp() {
        // এখানে আপনার অ্যাপের প্রারম্ভিক লজিকগুলো লিখুন
    }
}
