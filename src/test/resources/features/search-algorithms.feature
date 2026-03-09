Feature: Scientific resource search strategy comparison
  As a research archivist
  I want alternative filename search strategies to preserve the same matches as the dataset grows
  So that I can choose a reliable search approach for large scientific storage catalogues

  Background:
    Given the search benchmark owner is "researcher"

  Rule: Every search strategy must preserve the same matching files
    Scenario: Matching files are preserved for repeated scientific prefixes
      Given the search benchmark candidates are:
        | path | name               | size | type      |
        | lab/ | ababa-spectrum.csv | 120  | File      |
        | lab/ | beta-aba-notes.txt | 80   | File      |
        | lab/ | control-sample.txt | 64   | File      |
        | lab/ | aba-archive/       | 0    | Directory |
      When the filename search algorithms are compared for query "aba" with 20 repetitions
      Then both algorithms should return the following file names:
        | name               |
        | ababa-spectrum.csv |
        | beta-aba-notes.txt |
      And the comparison should agree with the reference substring matcher
      But each algorithm should have recorded a positive elapsed time

    Scenario Outline: Alternative strategies stay consistent under load
      Given the generated search benchmark dataset has <datasetSize> scientific files for query "<query>"
      When the filename search algorithms are compared for query "<query>" with <repetitions> repetitions
      Then both algorithms should return the same matches
      And the comparison should agree with the reference substring matcher
      And the benchmark should contain measurements for the following algorithms:
        | algorithm          |
        | Naive              |
        | Knuth-Morris-Pratt |
      But no algorithm run should be skipped

      Examples:
        | datasetSize | query   | repetitions |
        | 100         | aba     | 5           |
        | 100         | pattern | 5           |
        | 100         | sample  | 5           |

