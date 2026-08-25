/**
 * Sample JavaScript project for CodeSense AI parser tests.
 * Simple task management API.
 */

class TaskManager {
    constructor() {
        this.tasks = [];
        this.nextId = 1;
    }

    addTask(title, description) {
        if (!title) throw new Error('Task title is required');
        const task = {
            id: this.nextId++,
            title,
            description: description || '',
            completed: false,
            createdAt: new Date().toISOString()
        };
        this.tasks.push(task);
        return task;
    }

    completeTask(id) {
        const task = this.tasks.find(t => t.id === id);
        if (!task) throw new Error(`Task ${id} not found`);
        task.completed = true;
        return task;
    }

    deleteTask(id) {
        const index = this.tasks.findIndex(t => t.id === id);
        if (index === -1) throw new Error(`Task ${id} not found`);
        return this.tasks.splice(index, 1)[0];
    }

    getTasks(includeCompleted = false) {
        return includeCompleted
            ? this.tasks
            : this.tasks.filter(t => !t.completed);
    }
}

function createApiRouter(taskManager) {
    const express = require('express');
    const router = express.Router();

    router.get('/tasks', (req, res) => {
        const includeCompleted = req.query.all === 'true';
        res.json(taskManager.getTasks(includeCompleted));
    });

    router.post('/tasks', (req, res) => {
        const { title, description } = req.body;
        const task = taskManager.addTask(title, description);
        res.status(201).json(task);
    });

    router.patch('/tasks/:id/complete', (req, res) => {
        const task = taskManager.completeTask(parseInt(req.params.id));
        res.json(task);
    });

    return router;
}

module.exports = { TaskManager, createApiRouter };
