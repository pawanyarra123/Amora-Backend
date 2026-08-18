import time
from sqlalchemy import Column, Integer, String, Boolean, Float, Text
from app.db.session import Base


class ConversationTrainingModel(Base):
    """Stores every real Q&A pair so Amora can answer from trained data when needed."""
    __tablename__ = "conversation_training"

    id         = Column(Integer, primary_key=True, index=True)
    question   = Column(Text, nullable=False)   # what the user asked
    answer     = Column(Text, nullable=False)   # what Amora replied
    keywords   = Column(Text, default="")       # space-separated keywords for search
    use_count  = Column(Integer, default=1)     # how many times this was matched
    created_at = Column(Float, default=time.time)

class UserMemoryModel(Base):
    __tablename__ = "user_memories"

    id = Column(Integer, primary_key=True, index=True)
    memory_text = Column(Text, nullable=False)
    category = Column(String(50), default="general")
    created_at = Column(Float, default=time.time)

class CallSummaryModel(Base):
    __tablename__ = "call_summaries"

    id = Column(Integer, primary_key=True, index=True)
    caller_name = Column(String(100), nullable=False)
    phone_number = Column(String(30), nullable=False)
    reason = Column(Text, nullable=False)
    urgency = Column(String(20), default="normal")
    summary_text = Column(Text, nullable=False)
    created_at = Column(Float, default=time.time)

class IntruderLogModel(Base):
    __tablename__ = "intruder_logs"

    id = Column(Integer, primary_key=True, index=True)
    photo_path = Column(String(255), nullable=False)
    failure_reason = Column(String(100), default="Face auth failed")
    created_at = Column(Float, default=time.time)

class AutomationRuleModel(Base):
    __tablename__ = "automation_rules"

    id = Column(Integer, primary_key=True, index=True)
    rule_name = Column(String(100), nullable=False)
    trigger_type = Column(String(50), nullable=False)
    action_type = Column(String(50), nullable=False)
    is_enabled = Column(Boolean, default=True)
    created_at = Column(Float, default=time.time)
