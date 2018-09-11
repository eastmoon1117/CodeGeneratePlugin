package com.ccd;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CodeGenerate extends AnAction {
    private Project project;
    private VirtualFile virtualFile;
    private String packageName = ""; //包名
    private String mAuthor;//作者
    private String mModuleName;//模块名称
    private String mFunctionName;//功能名称

    private enum CodeType {
        Activity, Fragment, Contract, Presenter, Adapter, Component, PresenterModule, ActivityLayout, FragmentLayout
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getData(PlatformDataKeys.PROJECT);
        virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        packageName = getPackageNameByModule();//getPackageName();

        init();
        refreshProject(e);
    }

    /**
     * 刷新项目
     *
     * @param e
     */
    private void refreshProject(AnActionEvent e) {
        e.getProject().getBaseDir().refresh(false, true);
    }

    /**
     * 初始化Dialog
     */
    private void init() {
        MyCodeCreateDialog myDialog = new MyCodeCreateDialog(new MyCodeCreateDialog.DialogCallBack() {
            @Override
            public void ok(String author, String moduleName, String functionName) {
                mAuthor = author;
                mModuleName = moduleName;
                mFunctionName = functionName;
                if (author == null || author.trim().equals("")) {
                    Messages.showInfoMessage(project, "作者不能为空", "错误");
                    return;
                }
                if (moduleName == null || moduleName.trim().equals("")) {
                    Messages.showInfoMessage(project, "模块不能为空", "错误");
                    return;
                }
                if (functionName == null || functionName.trim().equals("")) {
                    Messages.showInfoMessage(project, "功能不能为空", "错误");
                    return;
                }

                if (!(virtualFile.getPath().contains(moduleName) && virtualFile.getPath().contains("src/main/java/"))) {
                    Messages.showInfoMessage(project, "请选择对应模块的目录再生成代码", "错误");
                    return;
                }

                String tmpModuleName = mModuleName.toLowerCase();
                if (tmpModuleName.contains("retail")) {
                    mModuleName = "retail_"+tmpModuleName.substring("retail".length());
                } else if (tmpModuleName.contains("catering")) {
                    mModuleName = "catering_"+tmpModuleName.substring("catering".length());
                }
                createClassFiles();
                Messages.showInfoMessage(project, "代码生成成功", "提示");
            }
        });
        myDialog.setVisible(true);
    }

    /**
     * 生成类文件
     */
    private void createClassFiles() {
        createClassFile(CodeType.Activity);
        createClassFile(CodeType.Fragment);
        createClassFile(CodeType.Contract);
        createClassFile(CodeType.Presenter);
        createClassFile(CodeType.Adapter);
        createClassFile(CodeType.Component);
        createClassFile(CodeType.PresenterModule);
        createClassFile(CodeType.ActivityLayout);
        createClassFile(CodeType.FragmentLayout);
    }

    /**
     * 生成mvp框架代码
     *
     * @param codeType
     */
    private void createClassFile(CodeType codeType) {
        String fileName = "";
        String content = "";
//        String appPath = getAppPath();
        String appPath = getModulePath();
//        String resPath = getResPath();
        String resPath = getModuleResPath();
        switch (codeType) {
            case Activity:
                fileName = "TemplateActivity.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/ui/", mFunctionName + "Activity.kt");
                break;
            case Fragment:
                fileName = "TemplateFragment.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/ui/", mFunctionName + "ListFragment.kt");
                break;
            case Contract:
                fileName = "TemplateContract.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/presenter/", mFunctionName + "Contract.kt");
                break;
            case Presenter:
                fileName = "TemplatePresenter.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/presenter/", mFunctionName + "Presenter.kt");
                break;
            case Adapter:
                fileName = "TemplateAdapter.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/adapter/", mFunctionName + "Adapter.kt");
                break;
            case Component:
                fileName = "TemplateComponent.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/presenter/dagger/", mFunctionName + "Component.kt");
                break;
            case PresenterModule:
                fileName = "TemplatePresenterModule.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mFunctionName.toLowerCase() + "/presenter/dagger/", mFunctionName + "PresenterModule.kt");
                break;
            case ActivityLayout:
                fileName = "activity_template.txt";
                content = ReadTemplateFile(fileName);
                writeToFile(content, resPath, "module_" + mModuleName.toLowerCase() + "_activity_" + mFunctionName.toLowerCase() + ".xml");
                break;
            case FragmentLayout:
                fileName = "fragment_template.txt";
                content = ReadTemplateFile(fileName);
                writeToFile(content, resPath, "module_" + mModuleName.toLowerCase() + "_fragment_" + mFunctionName.toLowerCase() + ".xml");
                break;
        }
    }

    /**
     * 获取包名文件路径
     *
     * @return
     */
    private String getAppPath() {
        String packagePath = packageName.replace(".", "/");
        String appPath = project.getBasePath() + "/App/src/main/java/" + packagePath + "/";
        return appPath;
    }

    /**
     * 当前鼠标选中的目录
     *
     * @return
     */
    private String getModulePath() {
        return virtualFile.getPath()+"/";
    }

    private String getModuleResPath() {
        int srcJava = virtualFile.getPath().indexOf("java");
        String androidResPath = virtualFile.getPath().substring(0, srcJava) + "/res/layout/";
        return androidResPath;
    }

    /**
     * 获取包名文件路径
     *
     * @return
     */
    private String getResPath() {
        String appPath = project.getBasePath() + "/App/src/main/res/layout/";
        return appPath;
    }

    /**
     * 替换模板中字符
     *
     * @param content
     * @return
     */
    private String dealTemplateContent(String content) {
        content = content.replace("$name", mFunctionName);
        if (content.contains("$packagename")) {
            content = content.replace("$packagename", packageName + "." + mFunctionName.toLowerCase());
        }
        if (content.contains("$basepackagename")) {
            content = content.replace("$basepackagename", packageName);
        }
        content = content.replace("$lowname", mFunctionName.toLowerCase());
        content = content.replace("$module", mModuleName.toLowerCase());
        content = content.replace("$author", mAuthor);
        content = content.replace("$date", getDate());
        return content;
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public String getDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 读取模板文件中的字符内容
     *
     * @param fileName 模板文件名
     * @return
     */
    private String ReadTemplateFile(String fileName) {
        InputStream in = null;
        in = this.getClass().getResourceAsStream("/com/ccd/Template/" + fileName);
        String content = "";
        try {
            content = new String(readStream(in));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream.close();
            inputStream.close();
        }

        return outputStream.toByteArray();
    }

    /**
     * 生成
     *
     * @param content   类中的内容
     * @param classPath 类文件路径
     * @param className 类文件名称
     */
    private void writeToFile(String content, String classPath, String className) {
        try {
            File floder = new File(classPath);
            if (!floder.exists()) {
                floder.mkdirs();
            }

            File file = new File(classPath + "/" + className);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 从AndroidManifest.xml文件中获取当前模块的包名
     *
     * @return
     */
    private String getPackageNameByModule() {
        int srcJava = virtualFile.getPath().indexOf("java");
        String androidManifestPath = virtualFile.getPath().substring(0, srcJava) + "AndroidManifest.xml";
        String package_name = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(androidManifestPath);

            NodeList nodeList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                package_name = element.getAttribute("package");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return package_name;
    }

    /**
     * 从AndroidManifest.xml文件中获取当前app的包名
     *
     * @return
     */
    private String getPackageName() {
        String package_name = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(project.getBasePath() + "/App/src/main/AndroidManifest.xml");

            NodeList nodeList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                package_name = element.getAttribute("package");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return package_name;
    }
}
