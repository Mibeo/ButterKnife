package com.zbj.annotation_compiler;

/**
 * Created by bingjia.zheng on 2019/8/6.
 */

import com.google.auto.service.AutoService;
import com.zbj.annotation.BindView;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

/**
 * 注解处理器，生成Activity相对应的类
 */

@AutoService(Processor.class)
//注册注解处理器
public class AnnotationCompiler extends AbstractProcessor {

    //生成文件的对象
    Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //初始化filer
        filer = processingEnvironment.getFiler();
    }

    /**
     * 声明这个注解处理器要处理的注解
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }

    /**
     * 声明当前注解处理器支持的java版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return processingEnv.getSourceVersion();
    }

    /**
     * 在这个方法里面我们就要去写文件
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //拿到整个模块中用到BindView注解的节点
        Set<? extends Element> elementsAnnotatedWith =
                roundEnvironment.getElementsAnnotatedWith(BindView.class);
        Map<String, List<VariableElement>> map = new HashMap<>();
        String activityName = null;
        for (Element element : elementsAnnotatedWith) {
            //获取到成员变量的节点 也就是控件
            VariableElement variableElement = (VariableElement) element;
            activityName = variableElement.getEnclosingElement().getSimpleName().toString();
            List<VariableElement> variableElements = map.get(activityName);
            if (variableElements == null) {
                variableElements = new ArrayList<>();
                map.put(activityName, variableElements);
            }
            variableElements.add(variableElement);
        }

        if (map.size() > 0) {
            //开始写文件
            Writer writer = null;
            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                List<VariableElement> variableElements = map.get(activityName);
                //通过控件的成员变量节点 获取到它的上一个节点也就是类节点
                TypeElement enclosingElement = (TypeElement) variableElements.get(0).getEnclosingElement();
                //通过成员变量得到包名
                String packageName = processingEnv.getElementUtils().getPackageOf(enclosingElement).toString();
                try {
                    JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + activityName + "_ViewBinding");
                    writer = sourceFile.openWriter();
                    writer.write("package " + packageName + ";\n");
                    writer.write("import " + packageName + ".IBinder;\n");
                    writer.write("public class " + activityName + "_ViewBinding implements IBinder<"
                            + packageName + "." + activityName + ">{\n");
                    writer.write("@Override\n" +
                            "    public void bind(" + packageName + "." + activityName + " target) {");
                    for (VariableElement variableElement : variableElements) {
                        //获取到控件的名字
                        String variableName = variableElement.getSimpleName().toString();
                        //获取到控件的ID
                        int id = variableElement.getAnnotation(BindView.class).value();
                        //获取到这个控件的类型
                        TypeMirror typeMirror = variableElement.asType();
                        writer.write("target." + variableName + "=(" + typeMirror + ")target.findViewById(" + id + ");\n");
                    }
                    writer.write("}\n}\n");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return false;
    }
}
