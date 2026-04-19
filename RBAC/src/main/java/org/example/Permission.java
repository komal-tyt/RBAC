package org.example;

public record Permission (String name, String resource, String description) {

    public Permission(String name, String resource, String description) {
        ValidationUtils.requireNonEmpty(name, "Permission name");
        ValidationUtils.requireNonEmpty(resource, "Resource");
        ValidationUtils.requireNonEmpty(description, "Description");

        if (name.contains(" ")) {
            throw new IllegalArgumentException("Permission name cannot contain spaces!");
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
