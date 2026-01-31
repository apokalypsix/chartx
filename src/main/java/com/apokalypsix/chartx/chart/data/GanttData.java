package com.apokalypsix.chartx.chart.data;

import com.apokalypsix.chartx.chart.data.DataListener;
import com.apokalypsix.chartx.core.data.DataListenerSupport;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Data for Gantt chart series.
 *
 * <p>Stores tasks with start/end times, dependencies, progress,
 * and optional milestones.
 */
public class GanttData {

    /**
     * Represents a single task in the Gantt chart.
     */
    public static class Task {
        private final String id;
        private String name;
        private long startTime;
        private long endTime;
        private float progress; // 0-1
        private Color color;
        private int row; // vertical position
        private String group; // optional grouping

        public Task(String id, String name, long startTime, long endTime) {
            this.id = id;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
            this.progress = 0f;
            this.color = null; // use default
            this.row = -1; // auto-assign
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public long getDuration() {
            return endTime - startTime;
        }

        public float getProgress() {
            return progress;
        }

        public void setProgress(float progress) {
            this.progress = Math.max(0, Math.min(1, progress));
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public boolean contains(long time) {
            return time >= startTime && time <= endTime;
        }
    }

    /**
     * Represents a milestone (point in time).
     */
    public static class Milestone {
        private final String id;
        private String name;
        private long time;
        private Color color;
        private int row;

        public Milestone(String id, String name, long time) {
            this.id = id;
            this.name = name;
            this.time = time;
            this.color = null;
            this.row = -1;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }
    }

    /**
     * Represents a dependency between tasks.
     */
    public static class Dependency {
        private final String fromTaskId;
        private final String toTaskId;
        private final DependencyType type;

        public Dependency(String fromTaskId, String toTaskId) {
            this(fromTaskId, toTaskId, DependencyType.FINISH_TO_START);
        }

        public Dependency(String fromTaskId, String toTaskId, DependencyType type) {
            this.fromTaskId = fromTaskId;
            this.toTaskId = toTaskId;
            this.type = type;
        }

        public String getFromTaskId() {
            return fromTaskId;
        }

        public String getToTaskId() {
            return toTaskId;
        }

        public DependencyType getType() {
            return type;
        }
    }

    public enum DependencyType {
        FINISH_TO_START,  // Most common
        START_TO_START,
        FINISH_TO_FINISH,
        START_TO_FINISH
    }

    private final String id;
    private final String name;

    private final List<Task> tasks;
    private final List<Milestone> milestones;
    private final List<Dependency> dependencies;

    // Cached bounds
    private long minTime = Long.MAX_VALUE;
    private long maxTime = Long.MIN_VALUE;
    private int maxRow = -1;
    private boolean boundsValid = false;

    private final DataListenerSupport listenerSupport = new DataListenerSupport();

    /**
     * Creates empty Gantt data.
     */
    public GanttData(String id, String name) {
        this.id = id;
        this.name = name;
        this.tasks = new ArrayList<>();
        this.milestones = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // ========== Task Management ==========

    /**
     * Adds a task to the chart.
     */
    public Task addTask(String taskId, String taskName, long startTime, long endTime) {
        Task task = new Task(taskId, taskName, startTime, endTime);
        tasks.add(task);
        boundsValid = false;
        listenerSupport.fireDataAppended(null, tasks.size() - 1);
        return task;
    }

    /**
     * Adds an existing task to the chart.
     */
    public void addTask(Task task) {
        tasks.add(task);
        boundsValid = false;
        listenerSupport.fireDataAppended(null, tasks.size() - 1);
    }

    /**
     * Returns all tasks.
     */
    public List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Returns a task by ID.
     */
    public Task getTask(String taskId) {
        for (Task task : tasks) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    /**
     * Returns the number of tasks.
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Removes a task by ID.
     */
    public boolean removeTask(String taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(taskId)) {
                tasks.remove(i);
                boundsValid = false;
                listenerSupport.fireDataUpdated(null, i);
                return true;
            }
        }
        return false;
    }

    // ========== Milestone Management ==========

    /**
     * Adds a milestone to the chart.
     */
    public Milestone addMilestone(String milestoneId, String milestoneName, long time) {
        Milestone milestone = new Milestone(milestoneId, milestoneName, time);
        milestones.add(milestone);
        boundsValid = false;
        return milestone;
    }

    /**
     * Returns all milestones.
     */
    public List<Milestone> getMilestones() {
        return new ArrayList<>(milestones);
    }

    // ========== Dependency Management ==========

    /**
     * Adds a dependency between tasks.
     */
    public void addDependency(String fromTaskId, String toTaskId) {
        dependencies.add(new Dependency(fromTaskId, toTaskId));
    }

    /**
     * Adds a typed dependency between tasks.
     */
    public void addDependency(String fromTaskId, String toTaskId, DependencyType type) {
        dependencies.add(new Dependency(fromTaskId, toTaskId, type));
    }

    /**
     * Returns all dependencies.
     */
    public List<Dependency> getDependencies() {
        return new ArrayList<>(dependencies);
    }

    /**
     * Returns dependencies originating from a task.
     */
    public List<Dependency> getDependenciesFrom(String taskId) {
        List<Dependency> result = new ArrayList<>();
        for (Dependency dep : dependencies) {
            if (dep.getFromTaskId().equals(taskId)) {
                result.add(dep);
            }
        }
        return result;
    }

    // ========== Bounds ==========

    private void updateBounds() {
        if (boundsValid) {
            return;
        }

        minTime = Long.MAX_VALUE;
        maxTime = Long.MIN_VALUE;
        maxRow = -1;

        for (Task task : tasks) {
            if (task.getStartTime() < minTime) {
                minTime = task.getStartTime();
            }
            if (task.getEndTime() > maxTime) {
                maxTime = task.getEndTime();
            }
            if (task.getRow() > maxRow) {
                maxRow = task.getRow();
            }
        }

        for (Milestone milestone : milestones) {
            if (milestone.getTime() < minTime) {
                minTime = milestone.getTime();
            }
            if (milestone.getTime() > maxTime) {
                maxTime = milestone.getTime();
            }
            if (milestone.getRow() > maxRow) {
                maxRow = milestone.getRow();
            }
        }

        if (tasks.isEmpty() && milestones.isEmpty()) {
            minTime = 0;
            maxTime = 0;
        }

        boundsValid = true;
    }

    /**
     * Returns the earliest start time.
     */
    public long getMinTime() {
        updateBounds();
        return minTime;
    }

    /**
     * Returns the latest end time.
     */
    public long getMaxTime() {
        updateBounds();
        return maxTime;
    }

    /**
     * Returns the time range (max - min).
     */
    public long getTimeRange() {
        updateBounds();
        return maxTime - minTime;
    }

    /**
     * Returns the maximum row index.
     */
    public int getMaxRow() {
        updateBounds();
        return maxRow;
    }

    /**
     * Returns the number of rows (max row + 1).
     */
    public int getRowCount() {
        updateBounds();
        return maxRow + 1;
    }

    // ========== Row Assignment ==========

    /**
     * Auto-assigns rows to tasks that don't have explicit row assignments.
     * Uses a simple greedy algorithm to minimize overlaps.
     */
    public void autoAssignRows() {
        // Sort tasks by start time
        List<Task> sortedTasks = new ArrayList<>(tasks);
        sortedTasks.sort((a, b) -> Long.compare(a.getStartTime(), b.getStartTime()));

        // Track row end times
        List<Long> rowEndTimes = new ArrayList<>();

        for (Task task : sortedTasks) {
            if (task.getRow() >= 0) {
                // Already assigned, ensure row exists
                while (rowEndTimes.size() <= task.getRow()) {
                    rowEndTimes.add(Long.MIN_VALUE);
                }
                rowEndTimes.set(task.getRow(), Math.max(rowEndTimes.get(task.getRow()), task.getEndTime()));
                continue;
            }

            // Find first available row
            int assignedRow = -1;
            for (int r = 0; r < rowEndTimes.size(); r++) {
                if (rowEndTimes.get(r) <= task.getStartTime()) {
                    assignedRow = r;
                    break;
                }
            }

            if (assignedRow < 0) {
                // Need new row
                assignedRow = rowEndTimes.size();
                rowEndTimes.add(Long.MIN_VALUE);
            }

            task.setRow(assignedRow);
            rowEndTimes.set(assignedRow, task.getEndTime());
        }

        // Assign milestones to their own rows or same as related tasks
        int milestoneRow = rowEndTimes.size();
        for (Milestone milestone : milestones) {
            if (milestone.getRow() < 0) {
                milestone.setRow(milestoneRow);
            }
        }

        boundsValid = false;
    }

    // ========== Queries ==========

    /**
     * Returns tasks visible in the given time range.
     */
    public List<Task> getTasksInRange(long startTime, long endTime) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getEndTime() >= startTime && task.getStartTime() <= endTime) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * Returns tasks at a specific row.
     */
    public List<Task> getTasksAtRow(int row) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getRow() == row) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * Finds the task at a given time and row.
     */
    public Task findTaskAt(long time, int row) {
        for (Task task : tasks) {
            if (task.getRow() == row && task.contains(time)) {
                return task;
            }
        }
        return null;
    }

    // ========== Listener Management ==========

    public void addListener(DataListener listener) {
        listenerSupport.addListener(listener);
    }

    public void removeListener(DataListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public String toString() {
        return String.format("GanttData[id=%s, tasks=%d, milestones=%d, dependencies=%d]",
                id, tasks.size(), milestones.size(), dependencies.size());
    }
}
