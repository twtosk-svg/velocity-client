package com.velocity;
import net.minecraft.entity.LimbAnimator;
public class TestLimb {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : LimbAnimator.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + m.getReturnType() + " " + m.getParameterCount());
        }
    }
}
