package mocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE
        // For now the local variable annotation is not used.
        // We use a configuration to map Method variable instead.
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {
    String value() default "";
}
