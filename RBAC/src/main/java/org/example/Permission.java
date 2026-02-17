package org.example;

public record Permission (String name, String resource, String description) {

    public Permission (String name, String resource, String description){

        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null!");
        }

        if (name.isEmpty()){
            throw new IllegalArgumentException("Name is blank!");
        }

        if (name.contains(" ")){
            throw new IllegalArgumentException("Name cannot contain spaces!");
        }

        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null!");
        }

        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Resource is empty!");
        }

        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null!");
        }

        if (description.isEmpty()){
            throw new IllegalArgumentException("Description is empty!");
        }

        this.name = name.toUpperCase();
        this.resource = resource.toLowerCase();
        this.description = description;
    }

    public String format() {
        return this.name + " on " + this.resource + ": " + this.description;
    }

    public boolean matches(String namePattern, String resourcePattern){
        return (namePattern == null || this.name.contains(namePattern)) && (resourcePattern == null || this.resource.contains(resourcePattern));
    }

}
