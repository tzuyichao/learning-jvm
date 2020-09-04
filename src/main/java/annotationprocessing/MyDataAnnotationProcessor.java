package annotationprocessing;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static com.sun.tools.javac.code.Flags.*;


/**
 * from 深入理解JVM bytecode example
 */
@SupportedAnnotationTypes({"annotationprocessing.MyData", "annotationprocessing.ToString"})
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
        Set<? extends Element> myDataElements = roundEnv.getElementsAnnotatedWith(MyData.class);
        processMyData(myDataElements);
        Set<? extends Element> toStringElements = roundEnv.getElementsAnnotatedWith(ToString.class);
        processToString(toStringElements);
        return true;
    }

    private void processMyData(Set<? extends Element> elements) {
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
                    jcClassDecl.defs = jcClassDecl.defs.prepend(genSerialVersionUIDVariable());
                    //jcClassDecl.defs = jcClassDecl.defs.prepend(genVariableMethod2());
                    jcClassDecl.defs = jcClassDecl.defs.prepend(genMethodWithInitVariable());
                }
            });
        }
    }

    private void processToString(Set<? extends Element> elements) {
        for(Element element: elements) {
            System.out.println("process ToString for " + element.getSimpleName().toString());
            JCTree tree = javacTrees.getTree(element);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    jcClassDecl.defs.stream()
                            .filter(it -> it.getKind().equals(Tree.Kind.VARIABLE))
                            .map(it -> (JCTree.JCVariableDecl)it)
                            .forEach(it -> {
                                System.out.println("name:" + it.getName() + ", type:" + it.vartype);
                            });
                    jcClassDecl.defs = jcClassDecl.defs.prepend(genToString());
                }
            });
        }
    }

    private JCTree.JCVariableDecl genSerialVersionUIDVariable() {
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(PRIVATE + STATIC + FINAL);
        Name id = names.fromString("serialVersionUID");
        JCTree.JCExpression varType = treeMaker.Type(new Type.JCPrimitiveType(TypeTag.LONG, null));
        JCTree.JCExpression init = treeMaker.Literal(1L);
        return treeMaker.VarDef(modifiers, id, varType, init);
    }

    private JCTree.JCMethodDecl genMethodWithInitVariable() {
        JCTree.JCVariableDecl valVariable = treeMaker.VarDef(
                treeMaker.Modifiers(PARAMETER),
                names.fromString("val"),
                query("java", "lang", "Long"),
                treeMaker.Literal(1L));
        System.out.println(valVariable.toString());
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>();
        JCTree.JCExpression systemOut = query("java", "lang", "System", "out");
        System.out.println(systemOut.toString());
        JCTree.JCExpressionStatement printToString = treeMaker.Exec(
                treeMaker.Apply(
                        com.sun.tools.javac.util.List.nil(),
                        treeMaker.Select(
                                systemOut,
                                names.fromString("println")),
                        com.sun.tools.javac.util.List.of(
                                treeMaker.Apply(
                                                com.sun.tools.javac.util.List.nil(),
                                                treeMaker.Select(
                                                        treeMaker.Ident(names._this),
                                                        names.fromString("toString")),
                                                com.sun.tools.javac.util.List.nil())
                        )
                )
        );
        statements.append(printToString);
        JCTree.JCExpressionStatement printToVal = treeMaker.Exec(
                treeMaker.Apply(
                        com.sun.tools.javac.util.List.nil(),
                        treeMaker.Select(
                                systemOut,
                                names.fromString("println")),
                        com.sun.tools.javac.util.List.of(treeMaker.Ident(names.fromString("val")))
                )
        );
        statements.append(printToVal);
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        JCTree.JCMethodDecl runMethodDecl = treeMaker.MethodDef(treeMaker.Modifiers(PUBLIC),
                names.fromString("run"),
                treeMaker.Type(new Type.JCVoidType()),
                com.sun.tools.javac.util.List.nil(),
                com.sun.tools.javac.util.List.of(valVariable),
                com.sun.tools.javac.util.List.nil(),
                body,
                null
        );
        System.out.println(runMethodDecl.toString());
        return runMethodDecl;
    }

    private JCTree.JCExpression query(String...namePath) {
        JCTree.JCExpression prev = treeMaker.Ident(names.fromString(namePath[0]));
        for(int i=1; i<namePath.length; i++) {
            prev = treeMaker.Select(prev, names.fromString(namePath[i]));
        }
        return prev;
    }

    private JCTree.JCMethodDecl genToString() {
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(PUBLIC);
        Name toStringName = names.fromString("toString");
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>();

        JCTree.JCExpression varType = query("java", "lang", "StringBuilder");

        Name result = names.fromString("result");
        JCTree.JCModifiers resultModifiers = treeMaker.Modifiers(Flags.PARAMETER);

        JCTree.JCNewClass stringBuilderClass = treeMaker.NewClass(null,
                com.sun.tools.javac.util.List.nil(),
                varType,
                com.sun.tools.javac.util.List.nil(),
                null);

        JCTree.JCVariableDecl resultIdent = treeMaker.VarDef(resultModifiers, result, varType, stringBuilderClass);
        System.out.println(resultIdent.toString());
        statements.append(resultIdent);

        JCTree.JCExpression runnableType = query("java", "lang", "Runnable");

        Name runnable = names.fromString("r");
        JCTree.JCModifiers rModifiers = treeMaker.Modifiers(Flags.PARAMETER);
        ListBuffer<JCTree.JCStatement> runStatements = new ListBuffer<>();
        JCTree.JCBlock rBody = treeMaker.Block(0, runStatements.toList());

        JCTree.JCMethodDecl runMethod = treeMaker.MethodDef(
                treeMaker.Modifiers(PUBLIC),
                names.fromString("run"),
                treeMaker.Type(new Type.JCVoidType()),
                com.sun.tools.javac.util.List.nil(),
                com.sun.tools.javac.util.List.nil(),
                com.sun.tools.javac.util.List.nil(),
                rBody,
                null
        );

        List<JCTree> defs = List.of(runMethod);

        JCTree.JCClassDecl classDecl = treeMaker.AnonymousClassDef(treeMaker.Modifiers(PARAMETER),
                defs);

        JCTree.JCNewClass rClass = treeMaker.NewClass(null,
                com.sun.tools.javac.util.List.nil(),
                runnableType,
                com.sun.tools.javac.util.List.nil(),
                classDecl);

        JCTree.JCVariableDecl rIdent = treeMaker.VarDef(rModifiers, runnable, runnableType, rClass);
        System.out.println(rIdent.toString());

        statements.append(rIdent);

        Name runName = names.fromString("r2");
        JCTree.JCLambda lambda = treeMaker.Lambda(com.sun.tools.javac.util.List.nil(),
                rBody);
        JCTree.JCNewClass runnableClass = treeMaker.NewClass(lambda,
                com.sun.tools.javac.util.List.nil(),
                runnableType,
                com.sun.tools.javac.util.List.nil(),
                null);
        JCTree.JCVariableDecl runnableIdent = treeMaker.VarDef(rModifiers, runName, runnableType, lambda);
        System.out.println(runnableIdent.toString());
        statements.append(runnableIdent);

        JCTree.JCClassDecl enclosingClassDecl = treeMaker.ClassDef(
                treeMaker.Modifiers(PARAMETER),
                names.fromString("Test"),
                com.sun.tools.javac.util.List.nil(),
                query("java", "lang", "Object"),
                com.sun.tools.javac.util.List.nil(),
                com.sun.tools.javac.util.List.nil());
        System.out.println(enclosingClassDecl.toString());
        //statements.append(enclosingClassDecl);
        JCTree.JCNewClass testClass = treeMaker.NewClass(enclosingClassDecl.getExtendsClause(),
                com.sun.tools.javac.util.List.nil(),
                treeMaker.Type(enclosingClassDecl.type),
                com.sun.tools.javac.util.List.nil(),
                null);
        System.out.println(testClass.toString());
        JCTree.JCVariableDecl testIdent = treeMaker.VarDef(rModifiers, names.fromString("qq"), treeMaker.Type(enclosingClassDecl.type), testClass);
        System.out.println(testIdent.toString());
        //statements.append(testIdent);

        JCTree.JCExpressionStatement append = treeMaker.Exec(treeMaker.Apply(
                com.sun.tools.javac.util.List.nil(),
                treeMaker.Select(
                        treeMaker.Ident(names.fromString("result")),
                        names.fromString("append")),
                com.sun.tools.javac.util.List.of(treeMaker.Literal("test"))
        ));
        statements.append(append);

        JCTree.JCReturn returnStatement = treeMaker.Return(
                treeMaker.Apply(
                        com.sun.tools.javac.util.List.nil(),
                        treeMaker.Select(
                                treeMaker.Ident(names.fromString("result")),
                                names.fromString("toString")),
                        com.sun.tools.javac.util.List.nil()
                )
        );
        statements.append(returnStatement);
        System.out.println(statements);

        JCTree.JCExpression string = query("java", "lang", "String");

        JCTree.JCExpression returnMethodType = string;
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        List<JCTree.JCTypeParameter> methodGenericParamList = List.nil();
        List<JCTree.JCVariableDecl> parameterList = List.nil();
        List<JCTree.JCExpression> thrownCauseList = List.nil();
        return treeMaker.MethodDef(modifiers, toStringName, returnMethodType, methodGenericParamList, parameterList, thrownCauseList, body, null);
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
