package com.taskmanagement.model.repository;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.enums.TaskStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    public List<Task> findOverdue(){
        return find("select t from Task t where t.dueDate < ?1 and t.status not in ?2",
                LocalDate.now(), Arrays.asList(TaskStatus.DONE, TaskStatus.CANCELLED)).list();
    }

    public List<Task> findDueSoon(int days){
        LocalDate endDate = LocalDate.now().plusDays(days);
        return find("select t from Task t where t.dueDate <= ?1 AND t.dueDate >= ?2 and t.status NOT IN ?3",
                endDate, LocalDate.now(), Arrays.asList(TaskStatus.DONE, TaskStatus.CANCELLED)).list();
    }

    public List<Task> findByUser(Long id){
        return find("assignee.id = ?1", id).list();
    }

}
