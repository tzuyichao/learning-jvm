package compile;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

public class JavaCompilerTest {
    public static void main(String[] args) {
        Context context = new Context();
        Options.instance(context).put(Option.ENCODING, "UTF-8");

        JavaCompiler compiler = new JavaCompiler(context);
        compiler.genEndPos = true;
        compiler.keepComments = true;

        JCTree.JCCompilationUnit compilationUnit = compiler.parse("src/main/java/client/User.java");
        JavacTrees javacTrees = JavacTrees.instance(context);
        TreeMaker treeMaker = TreeMaker.instance(context);
        Names names = Names.instance(context);

        JCTree.JCExpression client = treeMaker.Ident(names.fromString("client"));
        JCTree.JCExpression user = treeMaker.Select(client, names.fromString("User"));
        System.out.println(user.toString());

    }
}
