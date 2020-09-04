package compile;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import java.net.URI;

public class JavaCompilerTest2 {
    public static void main(String[] args) {
        Context context = new Context();
        Options.instance(context).put(Option.ENCODING, "UTF-8");

        JavaCompiler compiler = new JavaCompiler(context);
        compiler.genEndPos = true;
        compiler.keepComments = true;

        JCTree.JCCompilationUnit compilationUnit = compiler.parse(new MySimpleJavaFileObject(URI.create("file:///D:/lab/learning-jvm/src/main/java/client/User.java")));

        compilationUnit.defs.stream()
                .forEach(jcTree -> {
                    System.out.println(jcTree.toString());
                    System.out.println(jcTree.getKind());
                    listVariable(jcTree);
                });


    }

    private static void listVariable(JCTree tree) {
        tree.accept(new TreeTranslator() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl classDecl) {
                classDecl.defs.stream()
                        .filter(it -> it.getKind().equals(Tree.Kind.VARIABLE))
                        .map(it -> (JCTree.JCVariableDecl) it)
                        .forEach(it -> {
                            System.out.println(it.getName() + ": " + it.getKind() + ": " + it.getType());
                        });
            }
        });
    }
}
