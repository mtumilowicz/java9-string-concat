# java9-string-concat
Overview of java String concatenation compilation: java 8 vs java 9.

_Reference_: http://www.pellegrino.link/2015/08/22/string-concatenation-with-java-8.html  
_Reference_: https://www.guardsquare.com/en/blog/string-concatenation-java-9-untangling-invokedynamic  
_Reference_: https://www.guardsquare.com/en/blog/string-concatenation-java-9-conversion-confusion  
_Reference_: https://arnaudroger.github.io/blog/2017/06/14/CompactStrings.html  
_Reference_: https://stackoverflow.com/questions/46512888/how-is-string-concatenation-implemented-in-java-9

# preface
String concatenation is one of the most well known caveat in Java.

## java 8
* all of the substrings building the final String are known at compile 
time:
    ```
    @Test
    public void nonLoopConcatenation() {
        String a = "a";
        String b = "b";
        System.out.println(a + b);
    }
    ```
    is compiled to:
    ```
    public nonLoopConcatenation()V
    @Lorg/junit/Test;()
     L0
      LINENUMBER 10 L0
      LDC "a"
      ASTORE 1
     L1
      LINENUMBER 11 L1
      LDC "b"
      ASTORE 2
     L2
      LINENUMBER 12 L2
      GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
      NEW java/lang/StringBuilder
      DUP
      INVOKESPECIAL java/lang/StringBuilder.<init> ()V
      ALOAD 1
      INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
      ALOAD 2
      INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
      INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
      INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
     L3
      LINENUMBER 13 L3
      RETURN
     L4
      LOCALVARIABLE this LStringConcatBenchmarkTest; L0 L4 0
      LOCALVARIABLE a Ljava/lang/String; L1 L4 1
      LOCALVARIABLE b Ljava/lang/String; L2 L4 2
      MAXSTACK = 3
      MAXLOCALS = 3
    ```
    the most important part is:
    ```
    NEW java/lang/StringBuilder
    DUP
    INVOKESPECIAL java/lang/StringBuilder.<init> ()V
    ALOAD 1
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    ALOAD 2
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ```
    and it is also a bytecode of:
    ```
    String a = "a";
    String b = "b";
    
    System.out.println(new StringBuilder().append(a).append(b));
    ```
    
    it is called: **static string concatenation optimisation**

* substrings building the final String are NOT known at compile 
  time
  ```
  @Test
  public void loopConcatenation() {
      long start = System.currentTimeMillis();
      
      String result = "";
      
      for (int i = 0; i < 50_000; i++) {
          result += i;
      }
  
      System.out.println(result);
  
      System.out.println(System.currentTimeMillis() - start);
  }
  ```
  is compiled to:
  ```
  public loopConcatenation()V
  @Lorg/junit/Test;()
   L0
    LINENUMBER 25 L0
    INVOKESTATIC java/lang/System.currentTimeMillis ()J
    LSTORE 1
   L1
    LINENUMBER 27 L1
    LDC ""
    ASTORE 3
   L2
    LINENUMBER 29 L2
    ICONST_0
    ISTORE 4
   L3
   FRAME APPEND [J java/lang/String I]
    ILOAD 4
    LDC 50000
    IF_ICMPGE L4
   L5
    LINENUMBER 30 L5
    NEW java/lang/StringBuilder
    DUP
    INVOKESPECIAL java/lang/StringBuilder.<init> ()V
    ALOAD 3
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    ILOAD 4
    INVOKEVIRTUAL java/lang/StringBuilder.append (I)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    ASTORE 3
   L6
    LINENUMBER 29 L6
    IINC 4 1
    GOTO L3
   L4
    LINENUMBER 33 L4
   FRAME CHOP 1
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    ALOAD 3
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L7
    LINENUMBER 35 L7
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    INVOKESTATIC java/lang/System.currentTimeMillis ()J
    LLOAD 1
    LSUB
    INVOKEVIRTUAL java/io/PrintStream.println (J)V
   L8
    LINENUMBER 36 L8
    RETURN
   L9
    LOCALVARIABLE i I L3 L4 4
    LOCALVARIABLE this LStringConcatBenchmarkTest; L0 L9 0
    LOCALVARIABLE start J L1 L9 1
    LOCALVARIABLE result Ljava/lang/String; L2 L9 3
    MAXSTACK = 5
    MAXLOCALS = 5
  ```
  where the most important part is:
  ```
  FRAME APPEND [J java/lang/String I]
   ILOAD 4
   LDC 50000
   IF_ICMPGE L4
  L5
   LINENUMBER 30 L5
   NEW java/lang/StringBuilder
   DUP
   INVOKESPECIAL java/lang/StringBuilder.<init> ()V
   ALOAD 3
   INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
   ILOAD 4
   INVOKEVIRTUAL java/lang/StringBuilder.append (I)Ljava/lang/StringBuilder;
   INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
   ASTORE 3
  L6
   LINENUMBER 29 L6
   IINC 4 1
   GOTO L3
  ```
  which is equivalent of `.java`:
  ```
  for (int i = 0; i < 50_000; i++) {
      StringBuilder sb = new StringBuilder();
      sb.append(result);
      sb.append(i);
      result = sb.toString();
  }  
  ```
### summary
* no loop
    ```
    @Test
    public void nonLoopConcatenation() {
        String a = "a";
        String b = "b";
        System.out.println(a + b);
    }
    ```
    is compiled to:
    ```
    @Test
    public void nonLoopConcatenation_usingStringBuilder() {
        String a = "a";
        String b = "b";
        
        System.out.println(new StringBuilder().append(a).append(b));
    }
    ```
* loop
    ```
    @Test
    public void loopConcatenation() {
        long start = System.currentTimeMillis();
        
        String result = "";
        
        for (int i = 0; i < 50_000; i++) {
            result += i;
        }

        System.out.println(result);

        System.out.println(System.currentTimeMillis() - start);
    }    
    ```
    is compiled to:
    ```
    @Test
    public void loopConcatenation_usingStringBuilder() {
        long start = System.currentTimeMillis();

        String result = "";

        for (int i = 0; i < 50_000; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(result);
            sb.append(i);
            result = sb.toString();
        }

        System.out.println(result);

        System.out.println(System.currentTimeMillis() - start);
    }    
    ```

### Using Java 8 - `loopConcatenation()` takes: `6000-7000` ms

## java 9
* all of the substrings building the final String are known at compile 
  time:
    ```
    @Test
    public void nonLoopConcatenation() {
        String a = "a";
        String b = "b";
        System.out.println(a + b);
    }
    ```
    is compiled to:
    ```
    public nonLoopConcatenation()V
    @Lorg/junit/Test;()
     L0
      LINENUMBER 10 L0
      LDC "a"
      ASTORE 1
     L1
      LINENUMBER 11 L1
      LDC "b"
      ASTORE 2
     L2
      LINENUMBER 12 L2
      GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
      ALOAD 1
      ALOAD 2
      INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; [
        // handle kind 0x6 : INVOKESTATIC
        java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
        // arguments:
        "\u0001\u0001"
      ]
      INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
     L3
      LINENUMBER 13 L3
      RETURN
     L4
      LOCALVARIABLE this LStringConcatBenchmarkTest; L0 L4 0
      LOCALVARIABLE a Ljava/lang/String; L1 L4 1
      LOCALVARIABLE b Ljava/lang/String; L2 L4 2
      MAXSTACK = 3
      MAXLOCALS = 3
    ```
    the most important part is:
    ```
    ALOAD 1
    ALOAD 2
    INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
      // arguments:
      "\u0001\u0001"
    ]
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    ```
    
    the main difference to java 8 style is:
    * `INVOKEDYNAMIC` (instead of `INVOKEVIRTUAL`)
    * `StringConcatFactory.makeConcatWithConstants` (instead of 
    `StringBuilder`)

* substrings building the final String are NOT known at compile 
    time
    ```
    @Test
    public void loopConcatenation() {
        long start = System.currentTimeMillis();
        
        String result = "";
        
        for (int i = 0; i < 50_000; i++) {
            result += i;
        }

        System.out.println(result);

        System.out.println(System.currentTimeMillis() - start);
    }    
    ```
    is compiled to:
    ```
    public loopConcatenation()V
    @Lorg/junit/Test;()
     L0
      LINENUMBER 25 L0
      INVOKESTATIC java/lang/System.currentTimeMillis ()J
      LSTORE 1
     L1
      LINENUMBER 27 L1
      LDC ""
      ASTORE 3
     L2
      LINENUMBER 29 L2
      ICONST_0
      ISTORE 4
     L3
     FRAME APPEND [J java/lang/String I]
      ILOAD 4
      LDC 50000
      IF_ICMPGE L4
     L5
      LINENUMBER 30 L5
      ALOAD 3
      ILOAD 4
      INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;I)Ljava/lang/String; [
        // handle kind 0x6 : INVOKESTATIC
        java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
        // arguments:
        "\u0001\u0001"
      ]
      ASTORE 3
     L6
      LINENUMBER 29 L6
      IINC 4 1
      GOTO L3
     L4
      LINENUMBER 33 L4
     FRAME CHOP 1
      GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
      ALOAD 3
      INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
     L7
      LINENUMBER 35 L7
      GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
      INVOKESTATIC java/lang/System.currentTimeMillis ()J
      LLOAD 1
      LSUB
      INVOKEVIRTUAL java/io/PrintStream.println (J)V
     L8
      LINENUMBER 36 L8
      RETURN
     L9
      LOCALVARIABLE i I L3 L4 4
      LOCALVARIABLE this LStringConcatBenchmarkTest; L0 L9 0
      LOCALVARIABLE start J L1 L9 1
      LOCALVARIABLE result Ljava/lang/String; L2 L9 3
      MAXSTACK = 5
      MAXLOCALS = 5
    ```
    So conclusions are similar to `nonLoopConcatenation()`

### motivations

The reason to change the compiler now in this way is, from the project 
description, to "enable future optimizations of String concatenation 
without requiring further changes to the bytecode emitted by javac.". 
Dynamic method invocation is an ideal solution for that challenge, as 
it delays method implementation to the runtime. The developers of the 
Java runtime can then improve the implementation of the factory class, 
without all other developers needing to recompile their projects.

Recall that dynamic method invocation in Java works as follows: 
first, the compiler places an invokedynamic bytecode instruction 
in your method body to indicate that we’re trying to use a dynamic 
method there. That indy instruction refers to a bootstrap method, 
which is a regular Java method that is stored in a special attribute 
in the class file. During runtime, this bootstrap method is called 
to dynamically create the method we’re trying to invoke and wrap it 
in a container object called a CallSite. Finally, the JVM extracts 
a MethodHandle for the newly generated method from the CallSite and 
executes the method, manipulating the stack as if it were a regular 
method invocation.

### stategies
`StringConcatFactory` offers different strategies to generate the 
CallSite divided in byte-code generator using ASM and 
MethodHandle-based one.

* **BC_SB**: generate the byte-code equivalent to what javac generates 
in Java 8.
* **BC_SB_SIZED**: generate the byte-code equivalent to what javac but 
try to estimate the initial size of the StringBuilder.
* **BC_SB_SIZED_EXACT**: generate the byte-code equivalent to what javac 
but compute the exact size of the StringBuilder.
* **MH_SB_SIZED**: combines MethodHandles that ends up calling the 
StringBuilder with an estimated initial size.
* **MH_SB_SIZED_EXACT**: combines MethodHandles that ends up calling the 
StringBuilder with an exact size.
* **MH_INLINE_SIZED_EXACT**: combines MethodHandles that creates directly 
the String with an exact size byte[] with no copy.
* The default and most performant one is **MH_INLINE_SIZED_EXACT** that 
can lead to 3 to 4 times performance improvement. You can override the 
Strategy on the command line by defining the property 
java.lang.invoke.stringConcat.

### summary
**How string concatenation is done is a runtime decision, not a compile 
time one anymore.**

### Using Java 9 `loopConcatenation()` takes: `1000-1500` ms.

# project description
We provide tests with benchmarks for above examples.

The default java version is 9, if you want to switch it to 8, you
have to modify `build.gradle`:
```
sourceCompatibility = 1.8
```

## summary
Please note that irrelevant of java version using of well-known 
`StringBuilder` construction (with preallocated space) is the best
possible approach to that problem.

```
@Test
public void loopConcatenation_usingStringBuilder_rightWay() {
    long start = System.currentTimeMillis();

    StringBuilder sb = new StringBuilder(60000);

    for (int i = 0; i < 50_000; i++) {
        sb.append(i);
    }

    System.out.println(sb);

    System.out.println(System.currentTimeMillis() - start);
}
```
**it takes just 40-50**