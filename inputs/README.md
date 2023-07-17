# Generate test cases from TLC outputs
```bash
python path_generator.py END_ACTION /path/to/file.dot /path/to/store/paths [POR]
```
Explanation to parameters:
    
    END_ACTION: The name of the end action for a path.
    /path/to/file.dot: The path to dot file generated by TLC model checking.
    /path/to/store/paths: The path to store test cases.
    [POR]: Whether to enable partial order reduction. Optional.