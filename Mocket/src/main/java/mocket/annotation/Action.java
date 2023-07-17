package mocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.METHOD,
        ElementType.CONSTRUCTOR
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
    ActionStage stage() default ActionStage.Unknown;
    String value() default "";
}
