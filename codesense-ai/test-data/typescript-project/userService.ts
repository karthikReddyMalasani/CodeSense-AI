/**
 * Sample TypeScript project for CodeSense AI parser tests.
 * User management with TypeScript interfaces and generics.
 */

interface User {
    id: number;
    name: string;
    email: string;
    role: 'admin' | 'user' | 'guest';
}

interface Repository<T> {
    findById(id: number): Promise<T | null>;
    findAll(): Promise<T[]>;
    save(entity: T): Promise<T>;
    delete(id: number): Promise<void>;
}

class UserRepository implements Repository<User> {
    private users: Map<number, User> = new Map();

    async findById(id: number): Promise<User | null> {
        return this.users.get(id) ?? null;
    }

    async findAll(): Promise<User[]> {
        return Array.from(this.users.values());
    }

    async save(user: User): Promise<User> {
        this.users.set(user.id, user);
        return user;
    }

    async delete(id: number): Promise<void> {
        this.users.delete(id);
    }
}

class UserService {
    constructor(private readonly repository: UserRepository) {}

    async createUser(name: string, email: string, role: User['role'] = 'user'): Promise<User> {
        const id = Date.now();
        return this.repository.save({ id, name, email, role });
    }

    async getUserById(id: number): Promise<User> {
        const user = await this.repository.findById(id);
        if (!user) throw new Error(`User ${id} not found`);
        return user;
    }

    async listUsers(): Promise<User[]> {
        return this.repository.findAll();
    }
}

export { User, Repository, UserRepository, UserService };
