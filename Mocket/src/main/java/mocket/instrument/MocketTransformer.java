package mocket.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class MocketTransformer implements ClassFileTransformer {

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className != null && Instrumenter.isIgnoredClass(className)) {
            return classfileBuffer;
        }
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new MocketClassVisitor(cw, className);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        if (PreMain.DEBUG) {
            try {
                File debugDir = new File("debug-inst");
                if (!debugDir.exists()) {
                    debugDir.mkdir();
                }
                File f = new File("debug-inst/" + className.replace("/", ".") + ".class");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(cw.toByteArray());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cw.toByteArray();
    }
}
