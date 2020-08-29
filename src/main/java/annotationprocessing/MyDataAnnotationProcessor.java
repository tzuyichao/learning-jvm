package annotationprocessing;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.TreeMaker;

import static com.sun.tools.javac.code.Flags.PUBLIC;


/**
 * from 深入理解JVM bytecode example
 */
@SupportedAnnotationTypes("annotationprocessing.MyData")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class MyDataAnnotationProcessor extends AbstractProcessor {
    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        javacTrees = JavacTrees.instance(context);
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(MyData.class);
        for(Element element: elements) {
            System.out.println("Processing " + element.getSimpleName().toString());
            JCTree tree = javacTrees.getTree(element);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    jcClassDecl.defs.stream()
                            .filter(it -> it.getKind().equals(Tree.Kind.VARIABLE))
                            .map(it -> (JCTree.JCVariableDecl)it)
                            .forEach(it -> {
                                System.out.println("processing variable: " + it.getName());
                                jcClassDecl.defs = jcClassDecl.defs.prepend(genGetterMethod(it));
                                jcClassDecl.defs = jcClassDecl.defs.prepend(genSetterMethod(it));
                            });
                }
            });
        }
        return true;
    }

    private JCTree.JCMethodDecl genGetterMethod(JCTree.JCVariableDecl jcVariableDecl) {
        System.out.println("Getter - " + jcVariableDecl.getName());
        JCTree.JCReturn returnStatement = treeMaker.Return(
                treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())
        );

        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>().append(returnStatement);
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(PUBLIC);
        Name getMethodName = getMethodName(jcVariableDecl.getName());
        JCTree.JCExpression returnMethodType = jcVariableDecl.vartype;
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        List<JCTree.JCTypeParameter> methodGenericParamList = List.nil();
        List<JCTree.JCVariableDecl> parameterList = List.nil();
        List<JCTree.JCExpression> thrownCauseList = List.nil();

        return treeMaker.MethodDef(modifiers, getMethodName, returnMethodType, methodGenericParamList, parameterList, thrownCauseList, body, null);
    }

    private JCTree.JCMethodDecl genSetterMethod(JCTree.JCVariableDecl jcVariableDecl) {
        System.out.println("Setter - " + jcVariableDecl.getName());
        JCTree.JCExpressionStatement statement =
                treeMaker.Exec(
                        treeMaker.Assign(
                                treeMaker.Select(
                                        treeMaker.Ident(names.fromString("this")),
                                        jcVariableDecl.getName()
                                ),
                                treeMaker.Ident(jcVariableDecl.getName())
                        )
                );
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>().append(statement);

        JCTree.JCVariableDecl param = treeMaker.VarDef(
                treeMaker.Modifiers(Flags.PARAMETER, List.nil()),
                jcVariableDecl.name,
                jcVariableDecl.vartype,
                null
        );

        JCTree.JCModifiers modifiers = treeMaker.Modifiers(PUBLIC);
        Name setMethodName = setMethodName(jcVariableDecl.getName());
        JCTree.JCExpression returnMethodType = treeMaker.Type(new Type.JCVoidType());
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        List<JCTree.JCTypeParameter> methodGenericParamList = List.nil();
        List<JCTree.JCVariableDecl> parameterList = List.of(param);
        List<JCTree.JCExpression> thrownCauseList = List.nil();

        return treeMaker.MethodDef(modifiers, setMethodName, returnMethodType, methodGenericParamList, parameterList, thrownCauseList, body, null);
    }

    private Name getMethodName(Name name) {
        String fieldName = name.toString();
        return names.fromString("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, name.length()));
    }

    private Name setMethodName(Name name) {
        String fieldName = name.toString();
        return names.fromString("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, name.length()));
    }
}
