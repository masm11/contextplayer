CREATE TABLE Configs (
    Id integer primary key autoincrement,
    key string unique index,
    value string
);
