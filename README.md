# Compiler Construction A01

This project is a basic compiler implemented in Java for the custom programming language with a `.fos` file extension. It demonstrates key compiler construction concepts, including building NFAs and DFAs using Thompson's and the subset construction algorithms, lexical analysis, and symbol table management.

## Features

- **NFA & DFA Construction**  
  - Builds NFAs for token definitions using Thompson's construction.
  - Converts the combined NFA into a DFA via the subset construction algorithm.
  - Displays the DFA transition table and the total number of DFA states.

- **Lexical Analysis**  
  - Tokenizes source code into keywords, identifiers, numeric literals (with decimal rounding to 5 places), operators, and punctuation.
  - Handles single-line (`//`) and multi-line (`/* ... */`) comments.
  - Skips extra whitespace and enforces case sensitivity (only lowercase letters are valid for identifiers).

- **Symbol Table**  
  - Implements a simple scoped symbol table to track identifiers (with type, memory location, and constant flag).

- **Error Handling**  
  - Uses a centralized error handler to report lexical errors with corresponding line numbers.

- **File Input**  
  - Reads source code from a `.fos` file (e.g., `input.fos`).

## Getting Started

### Prerequisites

- Java 8 or higher

### Setup

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Asim-Iqbal-Khan/Compiler-Construction-A01.git
   cd Compiler-Construction-A01
   ```

2. **Place your `.fos` source file in the repository directory.**  
   For example, create an `input.fos` file containing your code.

### Compilation and Execution

1. **Compile the project:**

   ```bash
   javac Main.java
   ```

2. **Run the compiler:**

   ```bash
   java Main
   ```

   The program will:
   - Read source code from `input.fos`
   - Print the DFA transition table and total DFA states
   - Tokenize the source code and display the tokens
   - Populate and print the symbol table

## Sample .fos Program

```fos
/* Sample .fos program */

INT a = 15;
DECIMAL pi = 3.1415926535;
a = a + 5;
output "The value of a is:";
output a;
output "The value of pi is:";
output pi;
```

## Project Structure

- **Main.java**  
  Contains the main logic for DFA/NFA construction, lexical analysis, and symbol table management.

- **Supporting Classes**  
  - `TokenDefinition`, `Token`  
  - `State`, `NFA`, `NFAHelper`  
  - `SubsetConstruction`, `DFA`, `DFAState`  
  - `Lexer`, `ErrorHandler`  
  - `Symbol`, `SymbolTable`


## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Feel free to adjust the content as needed. Enjoy building your compiler!
