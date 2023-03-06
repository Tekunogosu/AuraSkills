package com.archyx.aureliumskills.api;

import dev.aurelium.skills.api.AureliumSkills;
import dev.aurelium.skills.api.AureliumSkillsProvider;

import java.lang.reflect.Method;

public class ApiRegistrationUtil {

    private static final Method REGISTER_METHOD;
    private static final Method UNREGISTER_METHOD;

    static {
        try {
            REGISTER_METHOD = AureliumSkillsProvider.class.getDeclaredMethod("register", AureliumSkills.class);
            REGISTER_METHOD.setAccessible(true);

            UNREGISTER_METHOD = AureliumSkillsProvider.class.getDeclaredMethod("unregister");
            UNREGISTER_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void register(AureliumSkills instance) {
        try {
            REGISTER_METHOD.invoke(null, instance);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static void unregister() {
        try {
            UNREGISTER_METHOD.invoke(null);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }


}
