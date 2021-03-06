.. meta::
    :author: Cask Data, Inc.
    :copyright: Copyright © 2014-2016 Cask Data, Inc.
    :license: See LICENSE file in this repository

================
CDAP File Tailer
================

**Note:** *The File Tailer is no longer supported for CDAP 3.0. Most likely, it will still
work, but because File Tailer is not aware of CDAP Namespaces, it would only work with
the default namespace.*

File Tailer is a daemon process that performs tailing of sets of local files. As soon as a
new record has been appended to the end of a file that the daemon is monitoring, it sends
it to a Stream via the CDAP RESTful API.


Features
========

- Distributed as debian and rpm packages
- Loads properties from a configuration file
- Supports rotation of log files
- Persists state and is able to resume from first unsent record
- Dumps statistics info

Usage
=====
To install File Tailer, execute one of these commands:
 
- on Debian/Ubuntu systems::

    sudo apt-get install cdap-file-tailer-1.0.2.deb

- on RHEL/CentOS systems::

    sudo rpm -ivh --force cdap-file-tailer-1.0.2.rpm

Repository information can be found in the `Distributed CDAP Installation instructions
<http://docs.cask.co/cdap/current/en/admin-manual/installation/installation.html#packaging>`__.

Once installed, configure the daemon by editing the file::

  /etc/file-tailer/conf/file-tailer.properties

These parameters must be specified::

  pipes=<pipe1-name, pipe2-name, ... >
  pipes.<pipe1-name>.source.work_dir=<source-work-directory>
  pipes.<pipe1-name>.source.file_name=<source-file-name>
  pipes.<pipe1-name>.sink.stream_name=<stream-name>
  pipes.<pipe1-name>.sink.host=<host-name>
  pipes.<pipe1-name>.sink.port=<port-number>
  ...

Please note that the target file must be accessible to the File Tailer user.
To check, you can use the ``more`` command with the File Tailer user::

  sudo -u file-tailer more path_to_target_file

To start the daemon, execute the command::

  sudo service file-tailer start

To stop the daemon, execute the command::

  sudo service file-tailer stop

File Tailer stores log files in the ``/var/log/file-tailer`` directory.
PID, states and statistics are stored in the ``/var/run/file-tailer`` directory.


Example Configuration
=====================
This configuration file will have the File Tailer application monitor two different
directories for target log files. Logs from each directory will be sent to two separate
streams.

::

  # General pipe properties 
  # Comma-separated list of pipes to be configured
  pipes=app1pipe,app2pipe

  # Pipe 1 source properties
  # Working directory (where to monitor files)
  pipes.app1pipe.source.work_dir=/var/log/app1
  # Name of log file
  pipes.app1pipe.source.file_name=app1.log

  # Pipe 1 sink properties
  # Name of the stream
  pipes.app1pipe.sink.stream_name=app1Stream
  # Host name that is used by stream client
  pipes.app1pipe.sink.host=cdap_host.example.com
  # Host port that is used by stream client
  pipes.app1pipe.sink.port=11015

  # Pipe 2 source properties
  # Working directory (where to monitor files)
  pipes.app2pipe.source.work_dir=/var/log/app2
  # Name of log file
  pipes.app2pipe.source.file_name=app2.log

  # Pipe 2 sink properties
  # Name of the stream
  pipes.app2pipe.sink.stream_name=app1Stream
  # Host name that is used by stream client
  pipes.app2pipe.sink.host=cdap_host.example.com
  # Host port that is used by stream client
  pipes.app2pipe.sink.port=11015


Authentication Client
=====================
Once File Tailer is installed, configure the Authentication Client by editing the
properties file::

  /etc/file-tailer/conf/auth-client.properties

Authentication Client configuration parameters:

- ``pipes.<pipe-name>.sink.auth_client`` - classpath of authentication client class
- ``pipes.<pipe-name>.sink.auth_client_properties`` - path to authentication client
  properties file

Authentication Client Example Configuration
-------------------------------------------

::

  # User name
  security.auth.client.username=admin
  # User password
  security.auth.client.password=realtime


Additional Notes
================

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Configuration Parameter
     - Description
   * - ``daemon_dir``
     - Path to directory for storage of File Tailer state and metrics
   * - ``pipes``
     - List of all pipes, comma-separated
   * - ``pipes.<pipe-name>.name``
     - Name of the pipe
   * - ``pipes.<pipe-name>.state_file``
     - Name of file, used to save state
   * - ``pipes.<pipe-name>.statistics_file``
     - Name of file, used to save statistics
   * - ``pipes.<pipe-name>.queue_size``
     - Size of queue (default 1000), of stored log records, before sending them to Stream
   * - ``pipes.<pipe-name>.source.work_dir``
     - Path to directory being monitored for target log files
   * - ``pipes.<pipe-name>.source.file_name``
     - Name of target log file
   * - ``pipes.<pipe-name>.source.rotated_file_name_pattern``
     - Log file rollover pattern (default ``(.*)`` )
   * - ``pipes.<pipe-name>.source.charset_name``
     - Name of charset used by Stream Client for sending logs (default ``UTF-8``)
   * - ``pipes.<pipe-name>.source.record_separator``
     - Symbol that separates each log record (default ``\n``)
   * - ``pipes.<pipe-name>.source.sleep_interval``
     - Interval to sleep after reading all log data (default 3000 ms)
   * - ``pipes.<pipe-name>.source.failure_retry_limit``
     - Number of attempts to retry reading a log, if an error occurred while reading file
       data (default value is 0 for unlimited attempts)
   * - ``pipes.<pipe-name>.source.failure_sleep_interval``
     - Interval to sleep if an error occurred while reading the file data (default 60000 ms)
   * - ``pipes.<pipe-name>.sink.stream_name``
     - Name of target stream
   * - ``pipes.<pipe-name>.sink.host``
     - Server host
   * - ``pipes.<pipe-name>.sink.port``
     - Server port
   * - ``pipes.<pipe-name>.sink.ssl``
     - Secure Socket Layer mode \[``true|false``] (default ``false``)
   * - ``pipes.<pipe-name>.sink.apiKey``
     - SSL security key
   * - ``pipes.<pipe-name>.sink.writerPoolSize``
     - Number of threads with which Stream Client sends events (default 10)
   * - ``pipes.<pipe-name>.sink.version``
     - CDAP server version (default ``v2``)
   * - ``pipes.<pipe-name>.sink.packSize``
     - Number of logs sent at a time (default 1)
   * - ``pipes.<pipe-name>.sink.failure_retry_limit``
     - Number of attempts to retry sending logs, if an error occurred while reading file 
       data (default value is 0 for unlimited attempts)
   * - ``pipes.<pipe-name>.sink.failure_sleep_interval``
     - Interval to sleep if an error occurred while sending the logs (default 60000 ms)
