package mocket.instrument.runtime;

public class MappedVariable {
    String owner;
    String name;
    String descriptor;


    public MappedVariable(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    public boolean isSame(MappedVariable var) {
        return owner.equals(var.owner) && name.equals(var.name) && descriptor.equals(var.descriptor);
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return descriptor;
    }

    @Override
    public boolean equals(Object anotherVariable) {
        MappedVariable var = (MappedVariable) anotherVariable;
        return this.owner.equals(var.owner) && this.name.equals(var.name)
                    && this.descriptor.equals(var.descriptor);
    }
}
