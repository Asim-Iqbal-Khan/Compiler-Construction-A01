import java.util.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Main {
    public static void main(String[] args) {
        SymbolTable symbolTable = new SymbolTable();
        List<TokenDefinition> tokenDefs = new ArrayList<>();
        int priority = 1;

        // Comments
        tokenDefs.add(new TokenDefinition("single_comment", NFAHelper.buildSingleLineComment(), priority++));
        tokenDefs.add(new TokenDefinition("multi_comment", NFAHelper.buildMultiLineComment(), priority++));

        // Data types
        tokenDefs.add(new TokenDefinition("INT", NFAHelper.buildLiteral("INT"), priority++));
        tokenDefs.add(new TokenDefinition("DECIMAL", NFAHelper.buildLiteral("DECIMAL"), priority++));
        tokenDefs.add(new TokenDefinition("BOOL", NFAHelper.buildLiteral("BOOL"), priority++));
        tokenDefs.add(new TokenDefinition("CHAR", NFAHelper.buildLiteral("CHAR"), priority++));

        // I/O and strings
        tokenDefs.add(new TokenDefinition("input", NFAHelper.buildLiteral("input"), priority++));
        tokenDefs.add(new TokenDefinition("output", NFAHelper.buildLiteral("output"), priority++));
        tokenDefs.add(new TokenDefinition("string_literal", NFAHelper.buildStringLiteral(), priority++));

        // Numeric (decimal literal rounded to 5 places)
        tokenDefs.add(new TokenDefinition("decimal_literal", NFAHelper.buildDecimalLiteral(), priority++));

        // Operators and punctuation
        tokenDefs.add(new TokenDefinition("plus", NFAHelper.buildLiteral("+"), priority++));
        tokenDefs.add(new TokenDefinition("minus", NFAHelper.buildLiteral("-"), priority++));
        tokenDefs.add(new TokenDefinition("mult", NFAHelper.buildLiteral("*"), priority++));
        tokenDefs.add(new TokenDefinition("div", NFAHelper.buildLiteral("/"), priority++));
        tokenDefs.add(new TokenDefinition("mod", NFAHelper.buildLiteral("%"), priority++));
        tokenDefs.add(new TokenDefinition("exp", NFAHelper.buildLiteral("^"), priority++));
        tokenDefs.add(new TokenDefinition("lparen", NFAHelper.buildLiteral("("), priority++));
        tokenDefs.add(new TokenDefinition("rparen", NFAHelper.buildLiteral(")"), priority++));
        tokenDefs.add(new TokenDefinition("semicolon", NFAHelper.buildLiteral(";"), priority++));
        tokenDefs.add(new TokenDefinition("assign", NFAHelper.buildLiteral("="), priority++));

        // Alphanumeric token (identifiers or integers)
        tokenDefs.add(new TokenDefinition("alphanum", NFAHelper.buildAlphanum(), priority++));

        NFA combinedNFA = NFAHelper.combineNFAs(tokenDefs);
        DFA dfa = SubsetConstruction.convert(combinedNFA);

        System.out.println("DFA Transition Table:");
        dfa.printTransitionTable();
        System.out.println("Total DFA States: " + dfa.getStateCount());

        Lexer lexer = new Lexer(dfa, tokenDefs, symbolTable);

        // Read source code from a .fos file
        String fileName = "input.fos";
        StringBuilder inputBuilder = new StringBuilder();
        try {
            Scanner scanner = new Scanner(new File(fileName));
            while (scanner.hasNextLine()) {
                inputBuilder.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + fileName);
            return;
        }
        String sourceCode = inputBuilder.toString();

        List<Token> tokens = lexer.tokenize(sourceCode);
        System.out.println("\nTokens:");
        for (Token token : tokens) {
            System.out.println(token);
        }

        // Add identifiers to symbol table
        for (Token token : tokens) {
            if (token.type.equals("identifier") && symbolTable.lookup(token.lexeme) == null) {
                symbolTable.add(token.lexeme, "undefined");
            }
        }
        System.out.println("\nSymbol Table:");
        symbolTable.printTable();
    }
}

class TokenDefinition {
    String tokenName;
    NFA nfa;
    int priority;

    public TokenDefinition(String tokenName, NFA nfa, int priority) {
        this.tokenName = tokenName;
        this.nfa = nfa;
        this.priority = priority;
        nfa.accept.tokenType = tokenName;
        nfa.accept.tokenPriority = priority;
    }
}

class Token {
    String type;
    String lexeme;
    int lineNumber;

    public Token(String type, String lexeme, int lineNumber) {
        this.type = type;
        this.lexeme = lexeme;
        this.lineNumber = lineNumber;
    }

    public String toString() {
        return "<" + type + ", " + lexeme + "> at line " + lineNumber;
    }
}

class State {
    static int stateCounter = 0;
    int id;
    Map<Character, List<State>> transitions;
    List<State> epsilonTransitions;
    String tokenType;
    int tokenPriority;

    public State() {
        id = stateCounter++;
        transitions = new HashMap<>();
        epsilonTransitions = new ArrayList<>();
        tokenType = null;
        tokenPriority = Integer.MAX_VALUE;
    }

    public void addTransition(char symbol, State state) {
        transitions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(state);
    }

    public void addEpsilonTransition(State state) {
        epsilonTransitions.add(state);
    }
}

class NFA {
    State start;
    State accept;

    public NFA(State start, State accept) {
        this.start = start;
        this.accept = accept;
    }
}

class NFAHelper {
    public static NFA buildLiteral(String s) {
        NFA result = null;
        for (char c : s.toCharArray()) {
            NFA nfaChar = literal(c);
            result = (result == null) ? nfaChar : concat(result, nfaChar);
        }
        return result;
    }

    public static NFA literal(char c) {
        State start = new State();
        State accept = new State();
        start.addTransition(c, accept);
        return new NFA(start, accept);
    }

    public static NFA concat(NFA first, NFA second) {
        first.accept.addEpsilonTransition(second.start);
        return new NFA(first.start, second.accept);
    }

    public static NFA union(NFA a, NFA b) {
        State start = new State();
        State accept = new State();
        start.addEpsilonTransition(a.start);
        start.addEpsilonTransition(b.start);
        a.accept.addEpsilonTransition(accept);
        b.accept.addEpsilonTransition(accept);
        return new NFA(start, accept);
    }

    public static NFA kleene(NFA a) {
        State start = new State();
        State accept = new State();
        start.addEpsilonTransition(a.start);
        start.addEpsilonTransition(accept);
        a.accept.addEpsilonTransition(a.start);
        a.accept.addEpsilonTransition(accept);
        return new NFA(start, accept);
    }

    public static NFA plus(NFA a) {
        return concat(a, kleene(a));
    }

    public static NFA buildRange(char startChar, char endChar) {
        NFA result = null;
        for (char c = startChar; c <= endChar; c++) {
            NFA nfaChar = literal(c);
            result = (result == null) ? nfaChar : union(result, nfaChar);
        }
        return result;
    }

    public static NFA buildAnyCharExcept(char excluded) {
        NFA result = null;
        for (char c = 32; c <= 126; c++) {
            if (c == excluded) continue;
            NFA nfaChar = literal(c);
            result = (result == null) ? nfaChar : union(result, nfaChar);
        }
        return result;
    }

    public static NFA anyChar() {
        NFA result = null;
        for (char c = 32; c <= 126; c++) {
            NFA nfaChar = literal(c);
            result = (result == null) ? nfaChar : union(result, nfaChar);
        }
        NFA newline = literal('\n');
        result = union(result, newline);
        return result;
    }

    public static NFA buildSingleLineComment() {
        NFA slashes = buildLiteral("//");
        NFA anyCharExceptNewline = buildAnyCharExcept('\n');
        NFA anyChars = kleene(anyCharExceptNewline);
        return concat(slashes, anyChars);
    }

    public static NFA buildMultiLineComment() {
        NFA startComment = buildLiteral("/*");
        NFA anyChars = kleene(anyChar());
        NFA endComment = buildLiteral("*/");
        return concat(concat(startComment, anyChars), endComment);
    }

    public static NFA buildStringLiteral() {
        NFA quote = literal('"');
        NFA notQuote = buildAnyCharExcept('"');
        NFA content = kleene(notQuote);
        return concat(concat(quote, content), quote);
    }

    public static NFA buildAlphanum() {
        NFA lower = buildRange('a', 'z');
        NFA upper = buildRange('A', 'Z');
        NFA digit = buildRange('0', '9');
        NFA letterOrDigit = union(union(lower, upper), digit);
        return plus(letterOrDigit);
    }

    public static NFA buildIntegerLiteral() {
        return buildRange('0', '9');
    }

    public static NFA buildDecimalLiteral() {
        NFA intPart = plus(buildRange('0', '9'));
        NFA dot = literal('.');
        NFA fracPart = plus(buildRange('0', '9'));
        return concat(concat(intPart, dot), fracPart);
    }

    public static NFA combineNFAs(List<TokenDefinition> tokenDefs) {
        State newStart = new State();
        for (TokenDefinition td : tokenDefs) {
            newStart.addEpsilonTransition(td.nfa.start);
        }
        return new NFA(newStart, null);
    }
}

class SubsetConstruction {
    public static DFA convert(NFA nfa) {
        Set<State> startSet = epsilonClosure(Collections.singleton(nfa.start));
        DFA dfa = new DFA();
        DFAState startDFAState = new DFAState(startSet);
        dfa.addState(startDFAState);
        dfa.setStartState(startDFAState);
        Queue<DFAState> queue = new LinkedList<>();
        queue.add(startDFAState);
        while (!queue.isEmpty()) {
            DFAState current = queue.poll();
            for (char c = 32; c <= 126; c++) {
                Set<State> moveResult = move(current.nfaStates, c);
                if (moveResult.isEmpty()) continue;
                Set<State> closure = epsilonClosure(moveResult);
                DFAState dfaState = dfa.getStateByNFAStates(closure);
                if (dfaState == null) {
                    dfaState = new DFAState(closure);
                    dfa.addState(dfaState);
                    queue.add(dfaState);
                }
                current.transitions.put(c, dfaState);
            }
        }
        return dfa;
    }

    private static Set<State> epsilonClosure(Set<State> states) {
        Stack<State> stack = new Stack<>();
        Set<State> closure = new HashSet<>(states);
        for (State s : states) stack.push(s);
        while (!stack.isEmpty()) {
            State s = stack.pop();
            for (State t : s.epsilonTransitions) {
                if (!closure.contains(t)) {
                    closure.add(t);
                    stack.push(t);
                }
            }
        }
        return closure;
    }

    private static Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        for (State s : states) {
            List<State> targets = s.transitions.get(symbol);
            if (targets != null) result.addAll(targets);
        }
        return result;
    }
}

class DFA {
    List<DFAState> states;
    DFAState startState;

    public DFA() {
        states = new ArrayList<>();
    }

    public void addState(DFAState s) {
        states.add(s);
    }

    public DFAState getStateByNFAStates(Set<State> nfaStates) {
        for (DFAState s : states) {
            if (s.nfaStates.equals(nfaStates)) return s;
        }
        return null;
    }

    public void setStartState(DFAState s) {
        startState = s;
    }

    public int getStateCount() {
        return states.size();
    }

    public void printTransitionTable() {
        System.out.println("State\tInput\tNextState");
        for (DFAState s : states) {
            for (Map.Entry<Character, DFAState> entry : s.transitions.entrySet()) {
                System.out.println(s.id + "\t" + printable(entry.getKey()) + "\t" + entry.getValue().id);
            }
        }
    }

    private String printable(char c) {
        if (c == ' ') return "space";
        if (c == '\n') return "\\n";
        return String.valueOf(c);
    }
}

class DFAState {
    static int dfaCounter = 0;
    int id;
    Set<State> nfaStates;
    Map<Character, DFAState> transitions;
    boolean isFinal;
    String tokenType;
    int tokenPriority;

    public DFAState(Set<State> states) {
        id = dfaCounter++;
        nfaStates = states;
        transitions = new HashMap<>();
        isFinal = false;
        tokenPriority = Integer.MAX_VALUE;
        tokenType = null;
        for (State s : states) {
            if (s.tokenType != null && s.tokenPriority < tokenPriority) {
                tokenPriority = s.tokenPriority;
                tokenType = s.tokenType;
                isFinal = true;
            }
        }
    }
}

class Lexer {
    DFA dfa;
    List<TokenDefinition> tokenDefs;
    SymbolTable symbolTable;

    public Lexer(DFA dfa, List<TokenDefinition> tokenDefs, SymbolTable symbolTable) {
        this.dfa = dfa;
        this.tokenDefs = tokenDefs;
        this.symbolTable = symbolTable;
    }

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0, lineNumber = 1;
        while (pos < input.length()) {
            if (Character.isWhitespace(input.charAt(pos))) {
                if (input.charAt(pos) == '\n') lineNumber++;
                pos++;
                continue;
            }
            if (input.startsWith("/*", pos)) {
                int endPos = input.indexOf("*/", pos + 2);
                if (endPos == -1) {
                    ErrorHandler.error("Unterminated multi-line comment.", lineNumber);
                    break;
                } else {
                    String commentContent = input.substring(pos, endPos + 2);
                    for (char ch : commentContent.toCharArray())
                        if (ch == '\n') lineNumber++;
                    pos = endPos + 2;
                    continue;
                }
            }
            if (input.startsWith("//", pos)) {
                int endPos = input.indexOf('\n', pos);
                pos = (endPos == -1) ? input.length() : endPos;
                continue;
            }
            int start = pos;
            DFAState currentState = dfa.startState, lastFinalState = null;
            int lastFinalPos = pos, currentPos = pos;
            while (currentPos < input.length()) {
                char c = input.charAt(currentPos);
                DFAState nextState = currentState.transitions.get(c);
                if (nextState == null) break;
                currentState = nextState;
                currentPos++;
                if (currentState.isFinal) {
                    lastFinalState = currentState;
                    lastFinalPos = currentPos;
                }
            }
            if (lastFinalState == null) {
                ErrorHandler.error("Unrecognized symbol: " + input.charAt(pos), lineNumber);
                pos++;
                continue;
            }
            String lexeme = input.substring(start, lastFinalPos);
            if (lastFinalState.tokenType.equals("decimal_literal")) {
                try {
                    BigDecimal bd = new BigDecimal(lexeme);
                    bd = bd.setScale(5, RoundingMode.HALF_UP);
                    lexeme = bd.toPlainString();
                } catch (Exception e) {
                    ErrorHandler.error("Invalid decimal literal: " + lexeme, lineNumber);
                }
                tokens.add(new Token(lastFinalState.tokenType, lexeme, lineNumber));
                pos = lastFinalPos;
                continue;
            }
            if (lastFinalState.tokenType.equals("alphanum")) {
                if (lexeme.matches("^[a-z]+$"))
                    tokens.add(new Token("identifier", lexeme, lineNumber));
                else if (lexeme.matches("^[0-9]+$"))
                    tokens.add(new Token("int_literal", lexeme, lineNumber));
                else
                    ErrorHandler.error("Invalid identifier: " + lexeme, lineNumber);
                pos = lastFinalPos;
                continue;
            }
            tokens.add(new Token(lastFinalState.tokenType, lexeme, lineNumber));
            pos = lastFinalPos;
        }
        return tokens;
    }
}

class ErrorHandler {
    public static void error(String message, int lineNumber) {
        System.err.println("Lexical error at line " + lineNumber + ": " + message);
    }
}

class Symbol {
    String name, type;
    int memoryLocation;
    boolean isConstant;

    public Symbol(String name, String type, int memoryLocation, boolean isConstant) {
        this.name = name;
        this.type = type;
        this.memoryLocation = memoryLocation;
        this.isConstant = isConstant;
    }

    public String toString() {
        return "Name: " + name + ", Type: " + type + ", Memory Location: " + memoryLocation + ", Constant: " + isConstant;
    }
}

class SymbolTable {
    private Deque<Map<String, Symbol>> scopes;
    private int nextMemoryLocation;

    public SymbolTable() {
        scopes = new ArrayDeque<>();
        scopes.push(new HashMap<>()); // global scope
        nextMemoryLocation = 0;
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() > 1) scopes.pop();
        else System.err.println("Cannot exit global scope");
    }

    public void add(String identifier, String type) {
        Map<String, Symbol> currentScope = scopes.peek();
        if (currentScope.containsKey(identifier))
            System.err.println("Symbol " + identifier + " already declared.");
        else {
            Symbol sym = new Symbol(identifier, type, nextMemoryLocation++, false);
            currentScope.put(identifier, sym);
        }
    }

    public Symbol lookup(String identifier) {
        for (Map<String, Symbol> scope : scopes)
            if (scope.containsKey(identifier)) return scope.get(identifier);
        return null;
    }

    public void printTable() {
        System.out.println("Symbol Table Contents:");
        List<Map<String, Symbol>> scopesList = new ArrayList<>(scopes);
        Collections.reverse(scopesList);
        for (Map<String, Symbol> scope : scopesList)
            for (Symbol sym : scope.values())
                System.out.println(sym);
    }
}
