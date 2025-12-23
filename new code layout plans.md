new code layout plans:

dev:
    - debugging
    - logging
    - commands just used for develepment

client:
    - any code needed for client side
    - has subfolders if there are many grouped features (like entity rendering (look into ways so that client side isnt needed))

data:
    - classes used for data storage and retrival
    - make class into multiple named subclasses so its not one massive class

entity:
    - all entity related code
    - has goals, maybe multple entity types for ease of goal switching

features:
    - sub folder for:
        - game setting related
        - world generation
        - big structures
        - small structures
        - interactions

mixins:
    - all mixins used in the project

utils:
    - all utility classes and methods
    - sub folders for:
        - multiplayer support
        - entity utils
        - world utils (blocks and structures)
        - calculations (line of sight and other math heavy stuff)

schedulers
    - all code related to scheduling tasks and events
    - maybe combine schedulers or make the way schedulers are times more centralized



