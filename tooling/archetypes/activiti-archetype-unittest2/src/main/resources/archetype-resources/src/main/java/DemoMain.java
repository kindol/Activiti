package ${package};

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class DemoMain {

    public static final Logger logger = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) throws ParseException {
        logger.info("program start!");

        /**
         *  步骤如下：
         *  1. 创建流程引擎（需要activiti.cfg.xml）
         *  2. 创建流程定义文件
         *  3. 启动运行流程
         *  4. 处理流程任务
         */

        // 1
        ProcessEngine processEngine = getProcessEngine();

        // 2
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        // 3
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        // 4
        processTask(processEngine, processInstance);
        logger.info("program end!");
    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> taskList = taskService.createTaskQuery().list();
            logger.info("待处理任务数量 [{}]", taskList.size());
            for (Task task: taskList) {
                logger.info("待处理任务 [{}]", task.getName());
                HashMap<String, Object> variables = getMap(processEngine, scanner, task);
                taskService.complete(task.getId(), variables);
                // 最后需要更新这个 instance，因为while循环判断了 isEnd()
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }

        scanner.close();
    }

    private static HashMap<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        // 输入一个任务所需要的内容要用到 FormService
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        HashMap<String, Object> variables = Maps.newHashMap();      // 保存当前任务所需要的输入
        for (FormProperty property: formProperties){
            String line = null;
            // 这里还需要加一些输入类型的判断，进行格式化
            if (StringFormType.class.isInstance(property.getType())){
                logger.info("please input {} ?", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else if (DateFormType.class.isInstance(property.getType())){
                logger.info("please input {} ? 格式（ yyyy-MM-dd ）", property.getName());
                line = scanner.nextLine();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(line);
                variables.put(property.getId(), date);
            } else {
                logger.info("type not support {}", property.getType());
            }
        }

        return variables;
    }

    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        // 启动、关闭流程需要运行时对象
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 通过流程定义文件中的 id 指定启动
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        logger.info("启动流程 [{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        // RepositoryService 可管理资源
        RepositoryService repositoryService = processEngine.getRepositoryService();
        // 获取资源对象并且deploy
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String id = deployment.getId();
        // 通过流程 id 获取流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(id)
                .singleResult();
        logger.info("流程定义文件 [{}], 流程ID [{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = processEngine.VERSION;
        logger.info("流程引擎名称[{}], 版本[{}]", name, version);
        return processEngine;
    }
}
