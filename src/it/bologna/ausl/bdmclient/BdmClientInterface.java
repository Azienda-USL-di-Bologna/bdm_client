package it.bologna.ausl.bdmclient;

import it.bologna.ausl.bdm.core.Bdm;
import it.bologna.ausl.bdm.core.BdmProcess;
import it.bologna.ausl.bdm.core.Step;
import it.bologna.ausl.bdm.core.Step.StepLogic;
import it.bologna.ausl.bdm.core.Task;
import it.bologna.ausl.bdm.exception.BdmExeption;
import it.bologna.ausl.bdm.utilities.Bag;
import java.util.List;

/**
 *
 * @author andrea
 */
public interface BdmClientInterface {

    /**
     * avvia un nuovo processo
     * @param processType è il tipo di processo es. ProcotolloInUscita0001
     * @param context contiene le informazioni tipo per il masterchef
     * @param parameters contiene le informazioni per il processo
     * @return Id del processo creato ed avviato
     * @throws it.bologna.ausl.bdm.exception.BdmExeption
     */
    public String startProcess(String processType, Bag context, Bag parameters) throws BdmExeption;

    /**
     * inserisce in task nello step passato
     * @param taskType
     * @param taskParameters
     * @param processId
     * @param stepId 
     * @return il taskId del nuovo task
     * @throws it.bologna.ausl.bdm.exception.BdmExeption
     */
    public String addTask(String taskType, Bag taskParameters, String processId, String stepId) throws BdmExeption;

    /**
     * rimuove uno task da uno step del processo
     * @param processId
     * @param stepId
     * @param taskId
     * @throws BdmExeption 
     */
    public void removeTask(String processId, String stepId, String taskId) throws BdmExeption;
    
    /**
     * rimuove una lista di task dallo step passato
     * @param processId
     * @param stepId
     * @param taskIdList lista id dei task da rimuovere
     * @throws BdmExeption 
     */
    public void removeTasks(String processId, String stepId, List<String> taskIdList) throws BdmExeption;
    
    /**
     * inserisce uno step nel processo
     * @param processId
     * @param stepDescription
     * @param stepLogic
     * @param stepType
     * @param allowedStepLogic la lista di step logic consentiti per lo step
     * @return lo stepId dello step inserito
     * @throws it.bologna.ausl.bdm.exception.BdmExeption
     */
    public String addStep(String processId, String stepDescription, StepLogic stepLogic, String stepType, List<Step.StepLogic> allowedStepLogic) throws BdmExeption;

    /**
     * rimuove uno step dal processo
     * @param processId
     * @param stepId
     * @throws BdmExeption 
     */
    public void removeStep(String processId, String stepId) throws BdmExeption;

    /**
     * cambia lo step logic su uno step
     * @param processId id del processo
     * @param stepId id dello step al quale cambiare lo StepLogic
     * @param stepLogic il nuovo StepLogic
     * @throws it.bologna.ausl.bdm.exception.BdmExeption
     */
    public void setStepLogic(String processId, String stepId, StepLogic stepLogic) throws BdmExeption;

    /**
     * @param processId id del processo del quale vogliamo recupare l'oggetto
     * Process
     * @return l'oggetto Process richiesto
     */
    public BdmProcess getProcess(String processId);

    /**
     * Restituisce lo step corrente del processo
     *
     * @param processId id del processo dal quale vogliamo recuperare lo step
     * @return lo step richiesto
     */
    public Step getCurrentStep(String processId);

    public Bdm.BdmStatus getProcessStatus(String processId);

    public Task getCurrentTask(String processId);

    public List<String> getForwardSteps(String processId);

    public List<String> getBackwardSteps(String processId);

    public Bag getContext(String processId);

    public void setContext(String processId, Bag context) throws BdmExeption;
    
    public void addInContext(String processId, Bag values) throws BdmExeption;

    public Bdm.BdmStatus stepOn(String processId, Bag parameters) throws BdmExeption;

    public Bdm.BdmStatus stepTo(String processId, String stepId, Bag parameters) throws BdmExeption;
    
    public Step getStep(String processId, String stepId);

    public Step getStepByType(String processId, String stepType);
    
    public Step getNextStep(String processId, String stepId);
    
    public void abortProcess(String processId) throws BdmExeption;
    
    public void deleteProcess(String processId) throws BdmExeption;
}
