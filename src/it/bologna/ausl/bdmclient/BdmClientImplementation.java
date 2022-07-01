package it.bologna.ausl.bdmclient;

import it.bologna.ausl.bdm.core.Bdm;
import it.bologna.ausl.bdm.core.Bdm.BdmStatus;
import it.bologna.ausl.bdm.core.BdmProcess;
import it.bologna.ausl.bdm.core.Step;
import it.bologna.ausl.bdm.core.Task;
import it.bologna.ausl.bdm.exception.BdmExeption;
import it.bologna.ausl.bdm.exception.ProcessWorkFlowException;
import it.bologna.ausl.bdm.exception.StorageException;
import it.bologna.ausl.bdm.processmanager.DbProcessStorageManager;
import it.bologna.ausl.bdm.processmanager.FileProcessStorageManager;
import it.bologna.ausl.bdm.utilities.Bag;
import it.bologna.ausl.bdm.processmanager.BdmProcessManager;
import java.io.File;
import java.util.List;


/**
 *
 * @author apasquini
 */
public class BdmClientImplementation implements BdmClientInterface {

    private final BdmProcessManager bdm;

    public BdmClientImplementation(File filePath) {
        FileProcessStorageManager fileStorage = new FileProcessStorageManager(filePath.getAbsolutePath());
        this.bdm = new BdmProcessManager(fileStorage);

    }

    public BdmClientImplementation(String dbUrl) throws StorageException {
        this.bdm = new BdmProcessManager(new DbProcessStorageManager(dbUrl));
    }


    @Override
    public String startProcess(String processType, Bag context, Bag parameters) throws BdmExeption {

        Bag additionalParamsBag = new Bag();
        additionalParamsBag.put(BdmProcessManager.ADDING_PROCESS_TYPE, processType);
        additionalParamsBag.put(BdmProcessManager.ADDING_PROCESS_PARAMS, context);

        BdmProcess createdProcess = this.bdm.addProcess(additionalParamsBag);
        BdmStatus status = this.bdm.stepOnProcess(createdProcess.getProcessId(), parameters);

        if (status == BdmStatus.ERROR)
            throw new ProcessWorkFlowException("Errore nello step-on del processo: " + createdProcess.getProcessId());
        return createdProcess.getProcessId();
    }

    @Override
    public BdmProcess getProcess(String processId) {
        return this.bdm.getProcess(processId);
    }

    @Override
    public Step getCurrentStep(String processId) {
        return getProcess(processId).getCurrentStep();
    }

    @Override
    public Bdm.BdmStatus getProcessStatus(String processId) {
        BdmProcess process = getProcess(processId);
        return process.getStatus();
    }

    @Override
    public Task getCurrentTask(String processId) {
        BdmProcess process = getProcess(processId);
        return process.getCurrentTask();
    }

    @Override
    public List<String> getForwardSteps(String processId) {
        BdmProcess process = getProcess(processId);
        return process.getForwardSteps();
    }

    @Override
    public List<String> getBackwardSteps(String processId) {
        BdmProcess process = getProcess(processId);
        return process.getBackwardSteps();
    }

    @Override
    public Bag getContext(String processId) {
        BdmProcess process = getProcess(processId);
        return process.getContext();
    }

    @Override
    public void setContext(String processId, Bag context) throws BdmExeption {
        bdm.setContext(processId, context);
    }

    @Override
    public void addInContext(String processId, Bag values) throws BdmExeption {
        bdm.addInContext(processId, values);
    }

    @Override
    public BdmStatus stepOn(String processId, Bag parameters) throws BdmExeption {
        BdmProcess process = getProcess(processId);
        Bdm.BdmStatus status = process.stepOn(parameters);
        return status;
    }

    @Override
    public BdmStatus stepTo(String processId, String stepId, Bag parameters) throws BdmExeption {
        BdmProcess process = getProcess(processId);
        Bdm.BdmStatus status = process.stepTo(stepId, parameters);
        return status;
    }

    @Override
    public Step getStep(String processId, String stepId) {
        return getProcess(processId).getStep(stepId);
    }

    @Override
    public Step getStepByType(String processId, String stepType) {
        return getProcess(processId).getStepByType(stepType);
    }

    @Override
    public Step getNextStep(String processId, String stepId) {
        return getProcess(processId).getNextStep(stepId);
    }

    @Override
    public String addTask(String taskType, Bag taskParameters, String processId, String stepId) throws BdmExeption {
        return bdm.addTask(taskType, taskParameters, processId, stepId);
    }

    @Override
    public String addStep(String processId, String stepDescription, Step.StepLogic stepLogic, String stepType, List<Step.StepLogic> allowedStepLogic) throws BdmExeption {
        return bdm.addStep(processId, stepDescription, stepLogic, stepType, allowedStepLogic);
    }

    @Override
    public void setStepLogic(String processId, String stepId, Step.StepLogic stepLogic) throws BdmExeption {
        bdm.setStepLogic(processId, stepId, stepLogic);
    }

    @Override
    public void abortProcess(String processId) throws StorageException {
        bdm.abortProcess(processId);
    }

    @Override
    public void deleteProcess(String processId) {
        bdm.deleteProcess(processId);
    }

    @Override
    public void removeTask(String processId, String stepId, String taskId) throws BdmExeption {
        bdm.removeTask(taskId, processId, stepId);
    }
    
    @Override
    public void removeTasks(String processId, String stepId, List<String> taskIdList) throws BdmExeption {
        for (String taskId : taskIdList) {
            bdm.removeTask(taskId, processId, stepId);        
        }
    }

    @Override
    public void removeStep(String processId, String stepId) throws BdmExeption {
        bdm.removeStep(stepId, processId);
    }
}