package org.awp0rtuh1ty.hearth;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HearthRule {
    String[] categories();
    String[] options() default {};
    boolean strict() default true;
}
