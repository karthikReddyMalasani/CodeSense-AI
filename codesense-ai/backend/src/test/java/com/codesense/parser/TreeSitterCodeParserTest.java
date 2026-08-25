package com.codesense.parser;

import com.codesense.parser.core.TreeSitterCodeParser;
import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Tree-sitter / regex-based multi-language parser.
 * Team Member 5 (Vishnu) — Testing.
 */
class TreeSitterCodeParserTest {

    private TreeSitterCodeParser parser;

    @BeforeEach
    void setUp() {
        parser = new TreeSitterCodeParser();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Python", "JavaScript", "TypeScript", "Go", "Rust", "Ruby", "Kotlin"})
    void supports_allExpectedLanguages(String language) {
        assertThat(parser.supports(language)).isTrue();
    }

    @Test
    void parse_python_extractsClassesAndFunctions() {
        String code = """
            from typing import List
            import hashlib
            
            class UserService:
                def __init__(self, repository):
                    self.repository = repository
                
                def find_all(self):
                    return self.repository.find_all()
                
                def find_by_id(self, user_id: int):
                    return self.repository.find_by_id(user_id)
            
            def create_hash(password: str) -> str:
                return hashlib.sha256(password.encode()).hexdigest()
            """;

        ParsedFile result = parser.parse("user_service.py", code, "Python");

        assertThat(result.getLanguage()).isEqualTo("Python");
        assertThat(result.getElements()).isNotEmpty();

        Optional<CodeElement> cls = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.CLASS && e.getName().equals("UserService"))
            .findFirst();
        assertThat(cls).isPresent();

        List<CodeElement> methods = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.METHOD)
            .toList();
        assertThat(methods).extracting(CodeElement::getName).contains("find_all", "find_by_id");

        List<CodeElement> funcs = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.FUNCTION)
            .toList();
        assertThat(funcs).extracting(CodeElement::getName).contains("create_hash");

        List<CodeRelationship> imports = result.getRelationships().stream()
            .filter(r -> r.getType() == CodeRelationship.RelationshipType.IMPORTS)
            .toList();
        assertThat(imports).isNotEmpty();
    }

    @Test
    void parse_javascript_extractsClassesAndFunctions() {
        String code = """
            import { EventEmitter } from 'events';
            
            class TaskManager {
                constructor() {
                    this.tasks = [];
                }
                
                addTask(title) {
                    this.tasks.push({ title, done: false });
                }
                
                getTasks() {
                    return this.tasks;
                }
            }
            
            function createTask(title, description) {
                return { title, description, done: false };
            }
            
            const processTask = async (task) => {
                return { ...task, processed: true };
            };
            """;

        ParsedFile result = parser.parse("tasks.js", code, "JavaScript");

        assertThat(result.getLanguage()).isEqualTo("JavaScript");

        Optional<CodeElement> cls = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.CLASS)
            .findFirst();
        assertThat(cls).isPresent().get().extracting(CodeElement::getName).isEqualTo("TaskManager");

        List<CodeRelationship> imports = result.getRelationships().stream()
            .filter(r -> r.getType() == CodeRelationship.RelationshipType.IMPORTS)
            .toList();
        assertThat(imports).extracting(CodeRelationship::getTargetElement).contains("events");
    }

    @Test
    void parse_typescript_extractsInterfacesAndClasses() {
        String code = """
            import { Injectable } from '@angular/core';
            
            interface User {
                id: number;
                name: string;
            }
            
            class UserService {
                private users: User[] = [];
                
                addUser(user: User): void {
                    this.users.push(user);
                }
                
                getUsers(): User[] {
                    return this.users;
                }
            }
            """;

        ParsedFile result = parser.parse("user.service.ts", code, "TypeScript");

        assertThat(result.getLanguage()).isEqualTo("TypeScript");

        List<CodeElement> interfaces = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.INTERFACE)
            .toList();
        assertThat(interfaces).isNotEmpty();

        List<CodeElement> classes = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.CLASS)
            .toList();
        assertThat(classes).isNotEmpty();
    }

    @Test
    void parse_go_extractsFunctionsAndTypes() {
        String code = """
            package main
            
            import (
                "fmt"
                "net/http"
            )
            
            type User struct {
                ID   int
                Name string
            }
            
            func GetUser(w http.ResponseWriter, r *http.Request) {
                fmt.Fprintf(w, "User endpoint")
            }
            
            func main() {
                http.HandleFunc("/user", GetUser)
                http.ListenAndServe(":8080", nil)
            }
            """;

        ParsedFile result = parser.parse("main.go", code, "Go");

        assertThat(result.getLanguage()).isEqualTo("Go");

        List<CodeElement> funcs = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.FUNCTION)
            .toList();
        assertThat(funcs).extracting(CodeElement::getName).contains("GetUser", "main");

        List<CodeElement> structs = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.STRUCT)
            .toList();
        assertThat(structs).extracting(CodeElement::getName).contains("User");
    }

    @Test
    void parse_rust_extractsFunctionsAndStructs() {
        String code = """
            use std::collections::HashMap;
            
            struct UserStore {
                users: HashMap<u64, String>,
            }
            
            impl UserStore {
                fn new() -> Self {
                    UserStore { users: HashMap::new() }
                }
                
                pub fn add_user(&mut self, id: u64, name: String) {
                    self.users.insert(id, name);
                }
            }
            
            fn main() {
                let mut store = UserStore::new();
                store.add_user(1, String::from("Alice"));
            }
            """;

        ParsedFile result = parser.parse("main.rs", code, "Rust");

        assertThat(result.getLanguage()).isEqualTo("Rust");

        List<CodeElement> structs = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.STRUCT)
            .toList();
        assertThat(structs).extracting(CodeElement::getName).contains("UserStore");

        List<CodeElement> funcs = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.FUNCTION)
            .toList();
        assertThat(funcs).isNotEmpty();
    }

    @Test
    void parse_emptyContent_returnsEmptyResult() {
        ParsedFile result = parser.parse("empty.py", "", "Python");
        assertThat(result).isNotNull();
        assertThat(result.getElements()).isEmpty();
        assertThat(result.getRelationships()).isEmpty();
    }

    @Test
    void parse_nullContent_returnsEmptyResult() {
        ParsedFile result = parser.parse("null.py", null, "Python");
        assertThat(result).isNotNull();
    }
}
