### 背景
这是一个Android程序的代码注入工具，目前实现的功能是在每个方法的执行前和执行后分别打印Log，如下。

```
06-19 14:44:15.836  6536  6555 I codeInjector: com.tencent.wcdb.database.SQLiteDatabase.isMainThread(Unknown Source) IN
06-19 14:44:15.836  6536  6555 I codeInjector: com.tencent.wcdb.database.SQLiteDatabase.isMainThread(Unknown Source) OUT #1 cost:0ms
```

在方法进入时会打印INLog，在方法退出时会打印OUTLog，Log的TAG为codeInjector。

当前仅做过MAC系统的适配。

### 使用方法
1. 将需要代码注入的apk重命名为src.apk，替换根目录下的src.apk。
2. 运行inject.sh，工具会自动处理apk，最后生成新的apk为signed.apk，然后自动安装到手机。
3. 使用过程中如果需要关闭Log，执行以下命令。

	```
adb shell
echo "-s 0" > /data/tmp/log.txt
```

	同样的，打开Log需执行：

	```
adb shell
echo "-s 1" > /data/tmp/log.txt
```
4. 使用过程中如需定制Log内容，可修改LogUtils.java，修改后置于根目录即可。


### 原理解析
代码注入依赖dex2jar，工具。dex2jar将apk中的dex转换为jar文件的时候用了asm来生成java代码，本工具就是在这个转化过程中进行了相应的处理，使转换后的java代码中包含使用者自定义的代码。

在[dex2jar的开源代码](https://github.com/pxb1988/dex2jar)基础上，有两个地方做了修改，一个是方法执行前，在Dex2jar.java中。

```
new ExDex2Asm(exceptionHandler) {
            public void convertCode(DexMethodNode methodNode, MethodVisitor mv) {
                if ((readerConfig & DexFileReader.SKIP_CODE) != 0 && methodNode.method.getName().equals("<clinit>")) {
                    // also skip clinit
                    return;
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/icodeinjector/LogUtils", "preLog", "()V", false);
                super.convertCode(methodNode, mv);

            }
}
```

另一个是代码Return时，在IR2JConverter.java中

```
case RETURN: {
    Value v = ((UnopStmt) st).op;
    accept(v, asm);
    insertI2x(v.valueType, ir.ret, asm);
    asm.visitIntInsn(BIPUSH, returnNum);
    asm.visitVarInsn(ISTORE, 1);
    asm.visitVarInsn(ILOAD, 1);
    asm.visitMethodInsn(INVOKESTATIC, "com/icodeinjector/LogUtils", "postLog", "(I)V", false);
    returnNum += 1;
    asm.visitInsn(getOpcode(v, IRETURN));
    }
    break;
case RETURN_VOID:
    asm.visitIntInsn(BIPUSH, returnNum);
    asm.visitVarInsn(ISTORE, 1);
    asm.visitVarInsn(ILOAD, 1);
    asm.visitMethodInsn(INVOKESTATIC, "com/icodeinjector/LogUtils", "postLog", "(I)V", false);
    returnNum += 1;
    asm.visitInsn(RETURN);
    break;
```

修改后的代码也可在本git库中看到。


