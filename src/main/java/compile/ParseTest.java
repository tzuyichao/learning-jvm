package compile;

import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.util.Context;

/**
 * from dive into jvm bytecode
 */
public class ParseTest {
    public static void main(String[] args) {
        ScannerFactory scannerFactory = ScannerFactory.instance(new Context());
        Scanner scanner = scannerFactory.newScanner("int k = i + j;", false);

        scanner.nextToken();
        Tokens.Token token = scanner.token();
        while(token.kind != Tokens.TokenKind.EOF) {
            if(token.kind == Tokens.TokenKind.IDENTIFIER) {
                System.out.print(token.name() + " - ");
            }
            System.out.println(token.kind);
            scanner.nextToken();
            token = scanner.token();;
        }
    }
}
