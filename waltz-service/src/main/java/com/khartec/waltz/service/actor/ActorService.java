package com.khartec.waltz.service.actor;

import com.khartec.waltz.data.actor.ActorDao;
import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.EntityReference;
import com.khartec.waltz.model.LastUpdate;
import com.khartec.waltz.model.actor.*;
import com.khartec.waltz.model.command.CommandOutcome;
import com.khartec.waltz.model.command.CommandResponse;
import com.khartec.waltz.model.command.ImmutableCommandResponse;
import com.khartec.waltz.model.actor.ImmutableActorChangeCommand;
import com.khartec.waltz.model.actor.ActorChangeCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.khartec.waltz.common.Checks.checkNotNull;

@Service
public class ActorService {

    private final ActorDao actorDao;


    @Autowired
    public ActorService(ActorDao actorDao) {
        checkNotNull(actorDao, "actorDao cannot be null");

        this.actorDao = actorDao;
    }


    public List<Actor> findAll() {
        return actorDao.findAll();
    }


    public Actor getById(long id) {
        return actorDao.getById(id);
    }


    public Long create(ActorCreateCommand command, String username) {
        checkNotNull(command, "command cannot be null");
        checkNotNull(username, "username cannot be null");

        return actorDao.create(command, username);
    }


    public CommandResponse<ActorChangeCommand> update(ActorChangeCommand command, String username) {
        checkNotNull(command, "command cannot be null");
        checkNotNull(username, "username cannot be null");

        ImmutableActorChangeCommand updateCommand = ImmutableActorChangeCommand
                .copyOf(command)
                .withLastUpdate(LastUpdate.mkForUser(username));

        boolean success = actorDao.update(updateCommand);
        return ImmutableCommandResponse.<ActorChangeCommand>builder()
                .originalCommand(command)
                .entityReference(EntityReference.mkRef(EntityKind.ACTOR, command.id()))
                .outcome(success ? CommandOutcome.SUCCESS : CommandOutcome.FAILURE)
                .build();
    }


    public boolean delete(long id) {
        return actorDao.deleteIfNotUsed(id);
    }

}
