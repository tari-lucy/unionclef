package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;

import java.util.List;

public class StopCommand extends Command {

    public StopCommand() {
        super(List.of("stop", "cancel"), "Stop task runner (stops all automation)");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        Debug.logMessage("Stopped all tasks");
        mod.stopTasks();
        // "Stop all automation" includes the build queue: stopTasks ends the walk and the tower,
        // and the queue would start them again next tick for the cells it still owes (2026-11-19).
        int owed = kaptainwutax.tungsten.helpers.BlockPlaceHelper.queued();
        if (owed > 0) {
            kaptainwutax.tungsten.helpers.BlockPlaceHelper.clearQueue();
            Debug.logMessage("Build queue dropped (" + owed + " cells)");
        }
        finish();
    }
}
