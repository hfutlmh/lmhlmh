package adf.sample.extaction;

import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.sample.SampleModuleKey;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

public class ActionExtMove extends ExtAction {
    private List<EntityID> searchTargets;
    private boolean isRest;

    public ActionExtMove(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.searchTargets = new ArrayList<>();
        this.isRest = false;
    }

    @Override
    public ExtAction setTarget(EntityID... targets) {
        this.searchTargets.clear();
        if(targets == null) {
            return this;
        }
        boolean isMove = false;
        for(EntityID entityID : targets) {
            StandardEntity entity = this.worldInfo.getEntity(entityID);
            if(entity.getStandardURN().equals(StandardEntityURN.BLOCKADE)) {
                entity = this.worldInfo.getEntity(((Blockade)entity).getPosition());
            } else if(entity instanceof Human) {
                entity = this.worldInfo.getPosition((Human)entity);
            }

            if(entity.getStandardURN().equals(StandardEntityURN.REFUGE)) {
                this.searchTargets.add(entityID);
            } else if(entity instanceof Area) {
                this.searchTargets.add(entityID);
                isMove = true;
            }
        }
        this.isRest = !isMove;
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;
        if(this.searchTargets == null || this.searchTargets.isEmpty()) {
            return this;
        }

        Human agent = (Human)this.agentInfo.me();
        PathPlanning pathPlanning = null;
        if(agent.getStandardURN() == StandardEntityURN.AMBULANCE_TEAM) {
            pathPlanning = this.moduleManager.getModule(SampleModuleKey.AMBULANCE_MODULE_PATH_PLANNING);
        } else if(agent.getStandardURN() == StandardEntityURN.FIRE_BRIGADE) {
            pathPlanning = this.moduleManager.getModule(SampleModuleKey.FIRE_MODULE_PATH_PLANNING);
        } else if(agent.getStandardURN() == StandardEntityURN.POLICE_FORCE) {
            pathPlanning = this.moduleManager.getModule(SampleModuleKey.POLICE_MODULE_PATH_PLANNING);
        }
        if(pathPlanning != null) {
            List<EntityID> path = pathPlanning
                    .setFrom(agent.getPosition())
                    .setDestination(this.searchTargets)
                    .calc().getResult();
            if (path != null) {
                if(this.isRest) {
                    boolean canRest = true;
                    for(EntityID id : path) {
                        if(this.worldInfo.getEntity(id).getStandardURN() != StandardEntityURN.REFUGE) {
                            canRest = false;
                        }
                    }
                    if(canRest) {
                        this.result = new ActionRest();
                    } else {
                        this.result = new ActionMove(path);
                    }
                } else {
                    this.result = new ActionMove(path);
                }
            }
        }
        return this;
    }
}